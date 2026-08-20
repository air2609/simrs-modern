package com.vone.simrs.antrian;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0021 (ANTRIAN DOKTER / antrianPerDokter.zul).
 *
 * <p>
 * Migrasi dari legacy {@code AntrianPerDokterController} + {@code DoctorManagerImpl.getAntrian()}
 * + {@code MsDoctorDAO.getAntrianPasien()} — antrian pasien hari ini untuk
 * dokter/staff yang sedang login.
 */
@Service
public class AntrianPerDokterService {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public AntrianPerDokterService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Antrian pasien untuk dokter yang sedang login (migrasi getAntrianPasien). */
    public AntrianPerDokterResponse getQueue(String username) {
        Integer staffId = findStaffIdByUsername(username);
        if (staffId == null) {
            throw new IllegalArgumentException("Staff untuk user tidak ditemukan.");
        }
        String doctorName = findStaffName(staffId);

        List<DelayAntrianQueueRowResponse> rows = jdbcTemplate.query(
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
                staffId);

        return new AntrianPerDokterResponse(doctorName, rows);
    }

    private Integer findStaffIdByUsername(String username) {
        List<Integer> rows = jdbcTemplate.query(
                "select n_staff_id from ms_user where upper(v_user_name) = ?",
                (resultSet, rowNum) -> getNullableInteger(resultSet, "n_staff_id"),
                username == null ? "" : username.trim().toUpperCase());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String findStaffName(Integer staffId) {
        List<String> rows = jdbcTemplate.query(
                "select v_staff_name from ms_staff where n_staff_id = ?",
                (resultSet, rowNum) -> resultSet.getString("v_staff_name"),
                staffId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String toDisplayDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DISPLAY_DATE);
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }
}
