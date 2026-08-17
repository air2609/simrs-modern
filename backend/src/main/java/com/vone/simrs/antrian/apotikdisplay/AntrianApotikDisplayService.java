package com.vone.simrs.antrian.apotikdisplay;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0020 (papan display OBAT PASIEN SUDAH JADI).
 *
 * <p>
 * Migrasi dari legacy {@code AntrianApotikController} (ZK) +
 * {@code ApotikManagerImpl.getApotikAntrian()} /
 * {@code ApotikDAO.getObatJadi()}
 * + {@code DoctorManagerImpl.getDelayAntrian()}.
 *
 * <p>
 * <b>Catatan performa:</b> sama seperti {@code AntrianApotikService}, query
 * utama hanya menyentuh {@code tb_examination} tanpa JOIN, lalu data
 * pasien/MR/racikan diambil secara batch agar tidak memicu full table scan
 * pada tabel besar tersebut.
 */
@Service
public class AntrianApotikDisplayService {

    private static final int NOTE_VALIDATED = 2;
    private static final int ANTRIAN_READY = 1;
    private static final String NOTE_PREFIX = "J-APTK%";
    private static final int DEFAULT_DELAY_MILLIS = 5000;

    private final JdbcTemplate jdbcTemplate;

    public AntrianApotikDisplayService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AntrianApotikDisplayResponse getDisplayData() {
        List<AntrianApotikDisplayItemResponse> items = getReadyItems();
        AntrianMasterRow master = getMasterAntrian();

        String antrianText = master != null ? master.getRollingTextApotik() : null;
        int delayMillis = master != null && master.getDelayAntrian() != null
                ? master.getDelayAntrian() * 1000
                : DEFAULT_DELAY_MILLIS;

        return new AntrianApotikDisplayResponse(items, antrianText, delayMillis);
    }

    /**
     * Nota yang obatnya sudah jadi (antrian_status=1).
     * Sama persis dengan legacy {@code getObatJadi()}.
     */
    private List<AntrianApotikDisplayItemResponse> getReadyItems() {
        Timestamp startOfDay = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<ReadyNoteRow> notes = jdbcTemplate.query(
                "select nota.n_exam_id, nota.n_patient_id, nota.n_reg_id "
                        + "from tb_examination nota "
                        + "where nota.d_whn_create between ? and ? "
                        + "and nota.n_exam_status = ? "
                        + "and nota.v_note_no like ? "
                        + "and nota.antrian_status = ? "
                        + "order by nota.d_whn_change desc",
                (resultSet, rowNum) -> new ReadyNoteRow(
                        resultSet.getInt("n_exam_id"),
                        getNullableInteger(resultSet, "n_patient_id"),
                        getNullableInteger(resultSet, "n_reg_id")),
                startOfDay, now, NOTE_VALIDATED, NOTE_PREFIX, ANTRIAN_READY);

        if (notes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, String> patientNames = getPatientNames(notes);
        Map<Integer, String> mrCodes = getMrCodes(notes);
        Set<Integer> examIdsWithRacikan = getExamIdsWithRacikan(notes);

        return notes.stream()
                .map(note -> new AntrianApotikDisplayItemResponse(
                        patientNames.get(note.getPatientId()),
                        mrCodes.get(note.getRegistrationId()),
                        examIdsWithRacikan.contains(note.getExamId()) ? "RACIKAN" : "NON-RACIKAN"))
                .collect(Collectors.toList());
    }

    private Map<Integer, String> getPatientNames(List<ReadyNoteRow> notes) {
        Map<Integer, String> patientNames = new HashMap<>();
        List<Integer> patientIds = notes.stream()
                .map(ReadyNoteRow::getPatientId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (patientIds.isEmpty()) {
            return patientNames;
        }
        String inClause = String.join(",", Collections.nCopies(patientIds.size(), "?"));
        jdbcTemplate.query(
                "select n_patient_id, v_patient_name from ms_patient where n_patient_id in (" + inClause + ")",
                (resultSet, rowNum) -> {
                    patientNames.put(resultSet.getInt("n_patient_id"), resultSet.getString("v_patient_name"));
                    return null;
                },
                patientIds.toArray());
        return patientNames;
    }

    private Map<Integer, String> getMrCodes(List<ReadyNoteRow> notes) {
        Map<Integer, String> mrCodes = new HashMap<>();
        List<Integer> registrationIds = notes.stream()
                .map(ReadyNoteRow::getRegistrationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (registrationIds.isEmpty()) {
            return mrCodes;
        }
        String inClause = String.join(",", Collections.nCopies(registrationIds.size(), "?"));
        jdbcTemplate.query(
                "select reg.n_reg_id, mr.v_mr_code "
                        + "from tb_registration reg "
                        + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                        + "where reg.n_reg_id in (" + inClause + ")",
                (resultSet, rowNum) -> {
                    mrCodes.put(resultSet.getInt("n_reg_id"), resultSet.getString("v_mr_code"));
                    return null;
                },
                registrationIds.toArray());
        return mrCodes;
    }

    /**
     * Nota yang punya obat racikan (baris pada {@code tb_drug_ingredients}).
     * Sama persis dengan legacy {@code nota.getTbDrugIngredients().size() > 0}.
     */
    private Set<Integer> getExamIdsWithRacikan(List<ReadyNoteRow> notes) {
        List<Integer> examIds = notes.stream()
                .map(ReadyNoteRow::getExamId)
                .distinct()
                .collect(Collectors.toList());
        Set<Integer> result = new HashSet<>();
        if (examIds.isEmpty()) {
            return result;
        }
        String inClause = String.join(",", Collections.nCopies(examIds.size(), "?"));
        jdbcTemplate.query(
                "select distinct n_note_id from tb_drug_ingredients where n_note_id in (" + inClause + ")",
                (resultSet, rowNum) -> result.add(resultSet.getInt("n_note_id")),
                examIds.toArray());
        return result;
    }

    /**
     * Ambil master antrian (baris tunggal pada tabel ms_antrian).
     * Sama persis dengan legacy {@code getMasterAntrian()} /
     * {@code getDelayAntrian()}.
     */
    private AntrianMasterRow getMasterAntrian() {
        try {
            return jdbcTemplate.queryForObject(
                    "select delay_antrian, rolling_text_apotik from ms_antrian limit 1",
                    (resultSet, rowNum) -> new AntrianMasterRow(
                            getNullableInteger(resultSet, "delay_antrian"),
                            resultSet.getString("rolling_text_apotik")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String column) {
        try {
            int value = resultSet.getInt(column);
            return resultSet.wasNull() ? null : value;
        } catch (java.sql.SQLException e) {
            return null;
        }
    }

    /**
     * Row helper untuk hasil query nota yang obatnya sudah jadi.
     */
    private static class ReadyNoteRow {
        private final Integer examId;
        private final Integer patientId;
        private final Integer registrationId;

        ReadyNoteRow(Integer examId, Integer patientId, Integer registrationId) {
            this.examId = examId;
            this.patientId = patientId;
            this.registrationId = registrationId;
        }

        Integer getExamId() {
            return examId;
        }

        Integer getPatientId() {
            return patientId;
        }

        Integer getRegistrationId() {
            return registrationId;
        }
    }

    /**
     * Row helper untuk tabel ms_antrian.
     */
    private static class AntrianMasterRow {
        private final Integer delayAntrian;
        private final String rollingTextApotik;

        AntrianMasterRow(Integer delayAntrian, String rollingTextApotik) {
            this.delayAntrian = delayAntrian;
            this.rollingTextApotik = rollingTextApotik;
        }

        Integer getDelayAntrian() {
            return delayAntrian;
        }

        String getRollingTextApotik() {
            return rollingTextApotik;
        }
    }
}
