package com.vone.simrs.antrian;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0019 (ANTRIAN DOKTER / antrianDokter.zul).
 *
 * <p>
 * Migrasi dari legacy {@code AntrianDokterController} +
 * {@code DoctorManagerImpl.getAntrianDokter()/getAntrian()/getDelayAntrian()}.
 * Mengembalikan seluruh dokter berantrian beserta antriannya sekaligus;
 * perpindahan dokter per delay dilakukan di sisi tampilan (fungsional sama
 * dengan siklus flag_antrian legacy).
 */
@Service
public class AntrianDisplayService {

    private static final String HOSPITAL_NAME = "RS. TIARA SELLA";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public AntrianDisplayService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Data display: hospital name, delay, text berjalan, dokter + antrian. */
    public AntrianDisplayResponse getDisplay() {
        Integer delay = findDelayAntrian();
        String text = findRollingText();
        List<AntrianDisplayDoctorResponse> doctors = getDoctors();
        return new AntrianDisplayResponse(HOSPITAL_NAME, delay, text, doctors);
    }

    private Integer findDelayAntrian() {
        List<Integer> rows = jdbcTemplate.query(
                "select delay_antrian from ms_antrian limit 1",
                (resultSet, rowNum) -> getNullableInteger(resultSet, "delay_antrian"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String findRollingText() {
        List<String> rows = jdbcTemplate.query(
                "select rolling_text_doctor from ms_antrian limit 1",
                (resultSet, rowNum) -> resultSet.getString("rolling_text_doctor"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Dokter berantrian hari ini (doctor_view) + antrian masing-masing. */
    private List<AntrianDisplayDoctorResponse> getDoctors() {
        List<Integer> staffIds = jdbcTemplate.query(
                "select n_staff_id from doctor_view order by n_staff_id",
                (resultSet, rowNum) -> resultSet.getInt("n_staff_id"));
        List<AntrianDisplayDoctorResponse> doctors = new ArrayList<>();
        for (Integer staffId : staffIds) {
            String name = findStaffName(staffId);
            if (name == null) {
                continue;
            }
            doctors.add(new AntrianDisplayDoctorResponse(
                    staffId, name, getQueue(staffId)));
        }
        return doctors;
    }

    private String findStaffName(Integer staffId) {
        List<String> rows = jdbcTemplate.query(
                "select v_staff_name from ms_staff where n_staff_id = ?",
                (resultSet, rowNum) -> resultSet.getString("v_staff_name"),
                staffId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Antrian pasien per dokter (getAntrianPasien legacy). */
    private List<DelayAntrianQueueRowResponse> getQueue(Integer doctorId) {
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

    private String toDisplayDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DISPLAY_DATE);
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }
}
