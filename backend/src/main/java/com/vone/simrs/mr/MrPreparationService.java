package com.vone.simrs.mr;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0006 (PERSIAPAN DOKUMEN REKAM MEDIS).
 *
 * <p>
 * Migrasi dari legacy {@code PersiapanMR} (UI controller) +
 * {@code RajalManagerImpl}
 * + {@code TbRegistrationDAO.getRegistrationOldPatient()}:
 * <ul>
 * <li>{@code getPatientOldRegistration(mrStatus)} →
 * {@link #getPreparationData()}</li>
 * <li>{@code moveMR()} / {@code updateRegistration()} →
 * {@link #markReady(Integer)}</li>
 * </ul>
 *
 * <p>
 * Query mencari registrasi hari ini milik pasien lama (MR sudah punya
 * registrasi
 * sebelumnya), lalu memisahkannya berdasarkan {@code mr_status}: belum siap
 * ({@code null}) dan sudah siap ({@code = 1}).
 */
@Service
public class MrPreparationService {

    private static final int MR_NOT_READY = 0;
    private static final int MR_READY = 1;

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public MrPreparationService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    public MrPreparationResponse getPreparationData() {
        List<MrPreparationItemResponse> notReadyList = getOldPatientRegistrations(MR_NOT_READY);
        List<MrPreparationItemResponse> readyList = getOldPatientRegistrations(MR_READY);
        return new MrPreparationResponse(notReadyList, readyList);
    }

    /**
     * Sama persis dengan legacy
     * {@code TbRegistrationDAO.getRegistrationOldPatient()}:
     * registrasi hari ini dimana MR pasien tersebut sudah pernah registrasi
     * sebelumnya.
     */
    private List<MrPreparationItemResponse> getOldPatientRegistrations(int mrStatus) {
        Timestamp startOfDay = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Timestamp startOfNextDay = Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay());

        String statusFilter = mrStatus == MR_NOT_READY ? "oldr.mr_status is null" : "oldr.mr_status = 1";

        String sql = "select oldr.n_reg_id, mr.v_mr_code, patient.v_patient_name, unit.v_unit_name "
                + "from tb_registration oldr "
                + "join tb_medical_record mr on mr.n_mr_id = oldr.n_mr_id "
                + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                + "left join ms_unit unit on unit.n_unit_id = oldr.n_unit_id "
                + "where oldr.n_reg_id in ("
                + "  select q.n_reg_id from ("
                + "    select r.n_reg_id, r.n_mr_id, "
                + "      (select count(1) from tb_registration r2 where r2.n_mr_id = r.n_mr_id and r2.d_registration_date < ?) as flag "
                + "    from tb_registration r "
                + "    where r.d_registration_date between ? and ?"
                + "  ) q where q.flag > 0"
                + ") "
                + "and " + statusFilter + " "
                + "order by oldr.d_registration_date asc";

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> new MrPreparationItemResponse(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        resultSet.getString("v_unit_name") != null ? resultSet.getString("v_unit_name") : "RAWAT INAP"),
                startOfDay, startOfDay, startOfNextDay);
    }

    /**
     * Tandai berkas rekam medis sebagai sudah siap.
     * Sama persis dengan legacy {@code PersiapanMR.moveMR()}: set
     * {@code mr_status = 1}.
     */
    @Transactional
    public void markReady(Integer regId) {
        if (regId == null) {
            throw new IllegalArgumentException("ID registrasi harus diisi.");
        }
        jdbcTemplate.update("update tb_registration set mr_status = ? where n_reg_id = ?", MR_READY, regId);
    }
}
