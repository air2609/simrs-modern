package com.vone.simrs.vitalsign;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.satusehat.SatuSehatOutboxService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * CRUD data tanda-tanda vital (TTV) per kunjungan (registrasi).
 */
@Service
public class VitalSignService {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final SatuSehatOutboxService satuSehatOutboxService;

    public VitalSignService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService,
            SatuSehatOutboxService satuSehatOutboxService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.satuSehatOutboxService = satuSehatOutboxService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    public List<VitalSignResponse> listByRegistration(String registrationCode) {
        if (!StringUtils.hasText(registrationCode)) {
            throw new IllegalArgumentException("KODE REGISTRASI HARUS DI ISI!");
        }
        return jdbcTemplate.query(
                "select t.n_ttv_id, r.v_reg_secondary_id, t.d_ttv_date, t.n_systole, t.n_diastole, t.n_pulse, "
                        + "t.n_respiratory_rate, t.n_temperature, t.n_spo2, t.n_weight, t.n_height, "
                        + "t.n_pain_scale, t.v_consciousness, t.v_note "
                        + "from tb_ttv t "
                        + "join tb_registration r on r.n_reg_id = t.n_reg_id "
                        + "where r.v_reg_secondary_id = ? "
                        + "order by t.d_ttv_date desc",
                (resultSet, rowNum) -> mapRow(resultSet),
                registrationCode.trim());
    }

    public VitalSignResponse save(VitalSignSaveRequest request, String username) {
        if (request == null || !StringUtils.hasText(request.getRegistrationCode())) {
            throw new IllegalArgumentException("KODE REGISTRASI HARUS DI ISI!");
        }
        RegistrationRef registration = resolveRegistration(request.getRegistrationCode().trim());
        Timestamp ttvDate = parseDate(request.getTtvDate());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String normalizedUser = username == null ? "" : username.trim().toUpperCase();

        Long id = jdbcTemplate.queryForObject("select nextval('tb_ttv_n_ttv_id_seq')", Long.class);
        jdbcTemplate.update(
                "insert into tb_ttv (n_ttv_id, n_reg_id, n_mr_id, n_patient_id, n_staff_id, d_ttv_date, "
                        + "n_systole, n_diastole, n_pulse, n_respiratory_rate, n_temperature, n_spo2, "
                        + "n_weight, n_height, n_pain_scale, v_consciousness, v_note, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, registration.regId, registration.mrId, registration.patientId, registration.staffId, ttvDate,
                request.getSystole(), request.getDiastole(), request.getPulse(), request.getRespiratoryRate(),
                request.getTemperature(), request.getSpo2(), request.getWeight(), request.getHeight(),
                request.getPainScale(), normalize(request.getConsciousness()), normalize(request.getNote()),
                normalizedUser, now);

        try {
            satuSehatOutboxService.enqueue(SatuSehatOutboxService.JOB_TTV,
                    request.getRegistrationCode().trim(), normalizedUser);
        } catch (RuntimeException ignored) {
            // jangan gagalkan penyimpanan TTV bila enqueue outbox bermasalah
        }

        return new VitalSignResponse(id, request.getRegistrationCode().trim(),
                DISPLAY.format(ttvDate.toLocalDateTime()), request.getSystole(), request.getDiastole(),
                request.getPulse(), request.getRespiratoryRate(), request.getTemperature(), request.getSpo2(),
                request.getWeight(), request.getHeight(), request.getPainScale(),
                normalize(request.getConsciousness()), normalize(request.getNote()));
    }

    private VitalSignResponse mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        Timestamp date = resultSet.getTimestamp("d_ttv_date");
        return new VitalSignResponse(
                resultSet.getLong("n_ttv_id"),
                resultSet.getString("v_reg_secondary_id"),
                date == null ? null : DISPLAY.format(date.toLocalDateTime()),
                (Integer) resultSet.getObject("n_systole"),
                (Integer) resultSet.getObject("n_diastole"),
                (Integer) resultSet.getObject("n_pulse"),
                (Integer) resultSet.getObject("n_respiratory_rate"),
                resultSet.getBigDecimal("n_temperature"),
                (Integer) resultSet.getObject("n_spo2"),
                resultSet.getBigDecimal("n_weight"),
                resultSet.getBigDecimal("n_height"),
                (Integer) resultSet.getObject("n_pain_scale"),
                resultSet.getString("v_consciousness"),
                resultSet.getString("v_note"));
    }

    private RegistrationRef resolveRegistration(String registrationCode) {
        List<RegistrationRef> rows = jdbcTemplate.query(
                "select r.n_reg_id, r.n_mr_id, mr.n_patient_id, r.n_staff_id "
                        + "from tb_registration r "
                        + "join tb_medical_record mr on mr.n_mr_id = r.n_mr_id "
                        + "where r.v_reg_secondary_id = ? order by r.n_reg_id desc limit 1",
                (resultSet, rowNum) -> new RegistrationRef(
                        resultSet.getLong("n_reg_id"),
                        resultSet.getLong("n_mr_id"),
                        resultSet.getLong("n_patient_id"),
                        (Long) resultSet.getObject("n_staff_id")),
                registrationCode);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Registrasi tidak ditemukan: " + registrationCode);
        }
        return rows.get(0);
    }

    private Timestamp parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return new Timestamp(System.currentTimeMillis());
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(value.trim()));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Format tanggal TTV tidak valid: " + value + " (gunakan ISO, mis. 2026-08-25T10:58)");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static final class RegistrationRef {
        private final long regId;
        private final long mrId;
        private final long patientId;
        private final Long staffId;

        private RegistrationRef(long regId, long mrId, long patientId, Long staffId) {
            this.regId = regId;
            this.mrId = mrId;
            this.patientId = patientId;
            this.staffId = staffId;
        }
    }
}
