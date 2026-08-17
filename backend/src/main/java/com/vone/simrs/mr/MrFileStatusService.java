package com.vone.simrs.mr;

import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0081 (FORM BERKAS REKAM MEDIS).
 *
 * <p>
 * Migrasi dari legacy {@code MRViewController} +
 * {@code MedicalRecordManagerImpl}:
 * <ul>
 * <li>{@code viewMrStatus()} → {@link #getByStatus(String)}</li>
 * <li>{@code getMrStatus(INPUT_BY_MANUAL/INPUT_BY_SEARCH)} →
 * {@link #getByCode(String)}</li>
 * <li>{@code PatientController.searchPatient()} → delegasi ke
 * {@link MrBorrowRequestService#searchPatients}
 * (dialog pencarian pasien yang sama persis dengan screen SC0175)</li>
 * </ul>
 *
 * <p>
 * Screen ini bersifat read-only (monitoring status berkas), tidak ada mutasi
 * data.
 */
@Service
public class MrFileStatusService {

    private static final String TERSEDIA = "10";
    private static final String SEDANG_DIPINJAM = "8";
    private static final String AKAN_DIPINJAM = "9";

    private final JdbcTemplate jdbcTemplate;
    private final MrBorrowRequestService mrBorrowRequestService;

    public MrFileStatusService(JdbcTemplate jdbcTemplate, MrBorrowRequestService mrBorrowRequestService) {
        this.jdbcTemplate = jdbcTemplate;
        this.mrBorrowRequestService = mrBorrowRequestService;
    }

    public String requireUsername(HttpSession session) {
        return mrBorrowRequestService.requireUsername(session);
    }

    /**
     * Sama persis dengan legacy {@code PatientController.searchPatient()} (dialog
     * "PENCARIAN DATA PASIEN").
     */
    public List<MrBorrowSearchResultResponse> searchPatients(String mrCode, String patientName, String nik,
            String birthDate, String address) {
        return mrBorrowRequestService.searchPatients(mrCode, patientName, nik, birthDate, address);
    }

    /**
     * Sama persis dengan legacy {@code MedicalRecordManagerImpl.viewMrStatus()}:
     * daftar berkas
     * berdasarkan status yang dipilih (SEDANG DIPINJAM / AKAN DIPINJAM).
     */
    public List<MrFileStatusItemResponse> getByStatus(String status) {
        if (!SEDANG_DIPINJAM.equals(status) && !AKAN_DIPINJAM.equals(status)) {
            throw new IllegalArgumentException("Status berkas MR tidak valid.");
        }

        return jdbcTemplate.query(
                "select mr.v_mr_code, mr.v_mr_status, patient.v_patient_name, unit.v_unit_name "
                        + "from tb_medical_record mr "
                        + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                        + "left join ms_unit unit on unit.n_unit_id = mr.n_unit_id "
                        + "where mr.v_mr_status = ? "
                        + "order by patient.v_patient_name",
                (resultSet, rowNum) -> mapRow(resultSet),
                status);
    }

    /**
     * Sama persis dengan legacy {@code MedicalRecordManagerImpl.getMrStatus()}:
     * cari satu berkas
     * berdasarkan No. MR (input manual maupun hasil pilih dari dialog pencarian).
     */
    public MrFileStatusItemResponse getByCode(String rawCode) {
        String mrCode = normalizeMrCode(rawCode);
        if (mrCode == null) {
            throw new IllegalArgumentException("No. MR tidak valid.");
        }

        try {
            return jdbcTemplate.queryForObject(
                    "select mr.v_mr_code, mr.v_mr_status, patient.v_patient_name, unit.v_unit_name "
                            + "from tb_medical_record mr "
                            + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                            + "left join ms_unit unit on unit.n_unit_id = mr.n_unit_id "
                            + "where mr.v_mr_code = ?",
                    (resultSet, rowNum) -> mapRow(resultSet),
                    mrCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Berkas Rekam Medis dengan No. MR " + mrCode + " tidak ditemukan.");
        }
    }

    private MrFileStatusItemResponse mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        String status = resultSet.getString("v_mr_status");
        String unitName = resultSet.getString("v_unit_name");
        return new MrFileStatusItemResponse(
                resultSet.getString("v_mr_code"),
                resultSet.getString("v_patient_name"),
                statusLabel(status),
                locationLabel(status, unitName));
    }

    private String statusLabel(String status) {
        if (status == null || TERSEDIA.equals(status)) {
            return "TERSEDIA";
        }
        if (SEDANG_DIPINJAM.equals(status)) {
            return "SEDANG DIPINJAM";
        }
        return "AKAN DIPINJAM";
    }

    private String locationLabel(String status, String unitName) {
        if (unitName == null || status == null || AKAN_DIPINJAM.equals(status) || TERSEDIA.equals(status)) {
            return "REKAM MEDIS";
        }
        return unitName;
    }

    /**
     * Sama persis dengan legacy {@code MedisafeUtil.convertToMrCode(String)}.
     */
    private String normalizeMrCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return null;
        }
        String trimmed = rawCode.trim();
        if (trimmed.length() != 6 && trimmed.length() != 8) {
            return null;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.length() == 8 && (i == 2 || i == 5)) {
                continue;
            }
            char c = trimmed.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
        }
        if (trimmed.length() == 8) {
            return trimmed;
        }
        return trimmed.substring(0, 2) + "-" + trimmed.substring(2, 4) + "-" + trimmed.substring(4, 6);
    }
}
