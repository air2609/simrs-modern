package com.vone.simrs.antrian;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SCM0053 (MASTER ANTRIAN DOKTER / delayAntrian.zul).
 *
 * <p>
 * Migrasi dari legacy {@code MsAntrianController} + {@code DoctorManagerImpl}
 * (getMasterAntrian / saveAntrian / getAntrianDoctorController / getAntrian /
 * takeOutFromAntrian) + {@code MsDoctorDAO}.
 */
@Service
public class DelayAntrianService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public DelayAntrianService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Masters: ms_antrian (delay + text) + daftar dokter berantrian hari ini.
     */
    public DelayAntrianMastersResponse getMasters() {
        AntrianRow antrian = findAntrian();
        List<DelayAntrianDoctorResponse> doctors = getDoctors();
        return new DelayAntrianMastersResponse(
                antrian == null ? null : antrian.delay,
                antrian == null ? null : antrian.text,
                doctors);
    }

    private AntrianRow findAntrian() {
        List<AntrianRow> rows = jdbcTemplate.query(
                "select id_antrian, delay_antrian, rolling_text_doctor from ms_antrian limit 1",
                (resultSet, rowNum) -> new AntrianRow(
                        resultSet.getInt("id_antrian"),
                        getNullableInteger(resultSet, "delay_antrian"),
                        resultSet.getString("rolling_text_doctor")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Dokter yang punya registrasi antrian hari ini (doctor_view). */
    private List<DelayAntrianDoctorResponse> getDoctors() {
        return jdbcTemplate.query(
                "select st.n_staff_id, st.v_staff_name from ms_staff st "
                        + "where st.n_staff_id in (select n_staff_id from doctor_view) "
                        + "order by st.v_staff_name",
                (resultSet, rowNum) -> new DelayAntrianDoctorResponse(
                        resultSet.getInt("n_staff_id"),
                        resultSet.getString("v_staff_name")));
    }

    /** Simpan / ubah master antrian (upsert baris tunggal ms_antrian). */
    @Transactional
    public DelayAntrianActionResultResponse save(DelayAntrianSaveRequest request) {
        if (request.getDelayAntrian() == null) {
            throw new IllegalArgumentException("DELAY ANTRIAN HARUS DI ISI!");
        }
        AntrianRow existing = findAntrian();
        if (existing == null) {
            jdbcTemplate.update(
                    "insert into ms_antrian (id_antrian, delay_antrian, rolling_text_doctor) "
                            + "values (?, ?, ?)",
                    getNextSequence("ms_antrian_seq"),
                    request.getDelayAntrian(),
                    request.getTextAntrian());
        } else {
            jdbcTemplate.update(
                    "update ms_antrian set delay_antrian = ?, rolling_text_doctor = ? "
                            + "where id_antrian = ?",
                    request.getDelayAntrian(),
                    request.getTextAntrian(),
                    existing.id);
        }
        return new DelayAntrianActionResultResponse(true, "Data Sukses Disimpan...!");
    }

    /**
     * Antrian pasien untuk seorang dokter (registrasi hari ini yang belum
     * dipanggil). Migrasi dari {@code MsDoctorDAO.getAntrianPasien()}.
     */
    public List<DelayAntrianQueueRowResponse> getQueue(Integer doctorId) {
        if (doctorId == null) {
            throw new IllegalArgumentException("PILIH DOKTER TERLEBIH DAHULU!");
        }
        return jdbcTemplate.query(
                "select reg.n_reg_id, reg.n_escort_primary_id, reg.d_registration_date, "
                        + "mr.v_mr_code, pat.v_patient_name "
                        + "from tb_registration reg "
                        + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                        + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                        + "where reg.d_registration_date between (current_date::timestamp) and now() "
                        + "and reg.n_staff_id = ? and reg.n_transfer_reg_id is null "
                        + "and reg.antrian_status is null "
                        + "and reg.v_reg_secondary_id not like '%IGD%' "
                        + "order by reg.d_registration_date asc",
                (resultSet, rowNum) -> new DelayAntrianQueueRowResponse(
                        resultSet.getInt("n_reg_id"),
                        getNullableInteger(resultSet, "n_escort_primary_id"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        toDisplayDateTime(resultSet.getTimestamp("d_registration_date"))),
                doctorId);
    }

    /** Keluarkan pasien dari antrian (antrian_status = 1). */
    @Transactional
    public DelayAntrianActionResultResponse takeOut(Integer registrationId) {
        if (registrationId == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        jdbcTemplate.update(
                "update tb_registration set antrian_status = 1 where n_reg_id = ?",
                registrationId);
        return new DelayAntrianActionResultResponse(true, "Pasien dikeluarkan dari antrian.");
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private String toDisplayDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DISPLAY_DATE);
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class AntrianRow {
        private final int id;
        private final Integer delay;
        private final String text;

        private AntrianRow(int id, Integer delay, String text) {
            this.id = id;
            this.delay = delay;
            this.text = text;
        }
    }
}
