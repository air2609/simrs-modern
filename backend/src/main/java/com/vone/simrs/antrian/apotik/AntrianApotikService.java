package com.vone.simrs.antrian.apotik;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0054 (KONTROL ANTRIAN APOTIK).
 *
 * <p>
 * Migrasi dari legacy {@code ApotikManagerImpl} + {@code ApotikDAO}:
 * <ul>
 * <li>{@code getValidatedNoteForAntrian} → {@link #getAntrianData()}</li>
 * <li>{@code getTextAntrian} → bagian dari {@link #getAntrianData()}</li>
 * <li>{@code saveAntrian} → {@link #saveAntrianText(String)}</li>
 * <li>{@code moveToAntiranApotik} → {@link #moveToReady(Integer)}</li>
 * <li>{@code takeOutAntrianApotik} → {@link #takeOut(Integer)}</li>
 * </ul>
 *
 * <p>
 * <b>Catatan performa:</b> Query antrian sengaja TIDAK melakukan JOIN ke
 * {@code tb_registration} / {@code tb_medical_record} pada tabel utama
 * {@code tb_examination}. Tabel {@code tb_examination} sangat besar (jutaan
 * baris), dan menambahkan JOIN pada query utama membuat query planner memilih
 * full table scan sehingga query menggantung (hang) dan akhirnya koneksi DB
 * terputus — yang memunculkan error "No message available". Sama persis dengan
 * legacy {@code ApotikDAO.getValidatedNoteToday()} / {@code getObatJadi()}
 * yang hanya query {@code tb_examination} lalu mengambil data pasien/MR secara
 * terpisah (lazy loading Hibernate).
 */
@Service
public class AntrianApotikService {

    private static final int NOTE_VALIDATED = 2;
    private static final int ANTRIAN_READY = 1;
    private static final int ANTRIAN_TAKEN_OUT = 2;
    private static final String NOTE_PREFIX = "J-APTK%";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public AntrianApotikService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Ambil data antrian apotik: nota yang sudah divalidasi (belum masuk antrian),
     * nota yang obatnya sudah jadi (antrian_status=1), dan teks antrian.
     */
    public AntrianApotikResponse getAntrianData() {
        List<AntrianApotikNoteResponse> validatedNotes = getValidatedNotes();
        List<AntrianApotikNoteResponse> readyNotes = getReadyNotes();
        AntrianTextRow antrian = getMasterAntrian();

        boolean hasText = antrian != null && antrian.getRollingTextApotik() != null;
        String text = hasText ? antrian.getRollingTextApotik() : null;

        return new AntrianApotikResponse(validatedNotes, readyNotes, text, hasText);
    }

    /**
     * Nota yang sudah divalidasi hari ini dan belum masuk antrian (antrian_status
     * is null).
     *
     * <p>
     * Sama persis dengan legacy {@code getValidatedNoteToday()}: query hanya
     * {@code tb_examination} (tanpa JOIN) lalu data pasien & MR diambil terpisah
     * per nota. Ini menghindari full table scan pada tabel besar.
     */
    private List<AntrianApotikNoteResponse> getValidatedNotes() {
        Timestamp startOfDay = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<AntrianApotikNoteResponse> notes = jdbcTemplate.query(
                "select nota.n_exam_id, nota.v_note_no, nota.d_whn_create, nota.n_patient_id, nota.n_reg_id "
                        + "from tb_examination nota "
                        + "where nota.d_whn_create between ? and ? "
                        + "and nota.n_exam_status = ? "
                        + "and nota.v_note_no like ? "
                        + "and nota.antrian_status is null "
                        + "order by nota.d_whn_create",
                (resultSet, rowNum) -> new AntrianApotikNoteResponse(
                        resultSet.getInt("n_exam_id"),
                        resultSet.getString("v_note_no"),
                        null,
                        null,
                        toTime(resultSet.getTimestamp("d_whn_create")),
                        resultSet.getInt("n_patient_id"),
                        getNullableInteger(resultSet, "n_reg_id")),
                startOfDay, now, NOTE_VALIDATED, NOTE_PREFIX);
        enrichPatientInfo(notes);
        return notes;
    }

    /**
     * Nota yang obatnya sudah jadi (antrian_status=1).
     *
     * <p>
     * Sama persis dengan legacy {@code getObatJadi()}: query hanya
     * {@code tb_examination} (tanpa JOIN) lalu data pasien & MR diambil terpisah.
     */
    private List<AntrianApotikNoteResponse> getReadyNotes() {
        Timestamp startOfDay = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<AntrianApotikNoteResponse> notes = jdbcTemplate.query(
                "select nota.n_exam_id, nota.v_note_no, nota.d_whn_create, nota.n_patient_id, nota.n_reg_id "
                        + "from tb_examination nota "
                        + "where nota.d_whn_create between ? and ? "
                        + "and nota.n_exam_status = ? "
                        + "and nota.v_note_no like ? "
                        + "and nota.antrian_status = ? "
                        + "order by nota.d_whn_change desc",
                (resultSet, rowNum) -> new AntrianApotikNoteResponse(
                        resultSet.getInt("n_exam_id"),
                        resultSet.getString("v_note_no"),
                        null,
                        null,
                        toTime(resultSet.getTimestamp("d_whn_create")),
                        resultSet.getInt("n_patient_id"),
                        getNullableInteger(resultSet, "n_reg_id")),
                startOfDay, now, NOTE_VALIDATED, NOTE_PREFIX, ANTRIAN_READY);
        enrichPatientInfo(notes);
        return notes;
    }

    /**
     * Isi nama pasien & kode MR untuk setiap nota.
     *
     * <p>
     * Data pasien dan MR diambil secara <b>batch</b> (satu query per tabel dengan
     * klausa {@code IN}) untuk menghindari pola N+1 yang membuat query menggantung
     * (hang) ketika jumlah nota banyak. Setiap lookup memakai {@code query()}
     * (bukan {@code queryForObject()}) sehingga baris yang tidak ditemukan
     * (data yatim / orphan pada tabel besar {@code tb_examination}) tidak
     * melempar {@code EmptyResultDataAccessException} — yang sebelumnya memicu
     * error "No message available".
     */
    private void enrichPatientInfo(List<AntrianApotikNoteResponse> notes) {
        if (notes.isEmpty()) {
            return;
        }

        // 1) Ambil nama pasien secara batch.
        Map<Integer, String> patientNames = new HashMap<>();
        List<Integer> patientIds = notes.stream()
                .map(AntrianApotikNoteResponse::getPatientId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!patientIds.isEmpty()) {
            String inClause = String.join(",", Collections.nCopies(patientIds.size(), "?"));
            jdbcTemplate.query(
                    "select n_patient_id, v_patient_name from ms_patient "
                            + "where n_patient_id in (" + inClause + ")",
                    (resultSet, rowNum) -> {
                        patientNames.put(resultSet.getInt("n_patient_id"),
                                resultSet.getString("v_patient_name"));
                        return null;
                    },
                    patientIds.toArray());
        }

        // 2) Ambil kode MR secara batch.
        Map<Integer, String> mrCodes = new HashMap<>();
        List<Integer> registrationIds = notes.stream()
                .map(AntrianApotikNoteResponse::getRegistrationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!registrationIds.isEmpty()) {
            String inClause = String.join(",", Collections.nCopies(registrationIds.size(), "?"));
            jdbcTemplate.query(
                    "select reg.n_reg_id, mr.v_mr_code "
                            + "from tb_registration reg "
                            + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                            + "where reg.n_reg_id in (" + inClause + ")",
                    (resultSet, rowNum) -> {
                        mrCodes.put(resultSet.getInt("n_reg_id"),
                                resultSet.getString("v_mr_code"));
                        return null;
                    },
                    registrationIds.toArray());
        }

        // 3) Terapkan hasil ke setiap nota.
        for (AntrianApotikNoteResponse note : notes) {
            if (note.getPatientId() != null) {
                note.setPatientName(patientNames.get(note.getPatientId()));
            }
            if (note.getRegistrationId() != null) {
                note.setMrCode(mrCodes.get(note.getRegistrationId()));
            }
        }
    }

    /**
     * Ambil master antrian (baris tunggal pada tabel ms_antrian).
     * Sama persis dengan legacy {@code getMasterAntrian()}.
     */
    private AntrianTextRow getMasterAntrian() {
        try {
            return jdbcTemplate.queryForObject(
                    "select id_antrian, rolling_text_apotik from ms_antrian limit 1",
                    (resultSet, rowNum) -> new AntrianTextRow(
                            resultSet.getInt("id_antrian"),
                            resultSet.getString("rolling_text_apotik")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Pindahkan nota dari daftar "sudah divalidasi" ke daftar "obat sudah jadi".
     * Sama persis dengan legacy {@code moveToAntiranApotik()} (antrian_status=1).
     */
    @Transactional
    public void moveToReady(Integer noteId) {
        if (noteId == null) {
            throw new IllegalArgumentException("ID nota harus diisi.");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_examination set antrian_status = ?, d_whn_change = ? where n_exam_id = ?",
                ANTRIAN_READY, now, noteId);
    }

    /**
     * Keluarkan nota dari daftar "obat sudah jadi" (antrian_status=2).
     * Sama persis dengan legacy {@code takeOutAntrianApotik()}.
     */
    @Transactional
    public void takeOut(Integer noteId) {
        if (noteId == null) {
            throw new IllegalArgumentException("ID nota harus diisi.");
        }
        jdbcTemplate.update(
                "update tb_examination set antrian_status = ? where n_exam_id = ?",
                ANTRIAN_TAKEN_OUT, noteId);
    }

    /**
     * Simpan teks antrian apotik (rolling_text_apotik).
     * Sama persis dengan legacy {@code saveAntrian()}.
     */
    @Transactional
    public void saveAntrianText(String antrianText) {
        AntrianTextRow antrian = getMasterAntrian();
        if (antrian == null) {
            throw new IllegalStateException("Master antrian tidak ditemukan.");
        }
        jdbcTemplate.update(
                "update ms_antrian set rolling_text_apotik = ? where id_antrian = ?",
                antrianText, antrian.getIdAntrian());
    }

    private String toTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().format(TIME_FORMAT);
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
     * Row helper untuk tabel ms_antrian.
     */
    private static class AntrianTextRow {
        private final Integer idAntrian;
        private final String rollingTextApotik;

        AntrianTextRow(Integer idAntrian, String rollingTextApotik) {
            this.idAntrian = idAntrian;
            this.rollingTextApotik = rollingTextApotik;
        }

        Integer getIdAntrian() {
            return idAntrian;
        }

        String getRollingTextApotik() {
            return rollingTextApotik;
        }
    }
}
