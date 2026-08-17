package com.vone.simrs.mr;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0175 (FORM PEMINJAMAN BERKAS REKAM MEDIS).
 *
 * <p>
 * Migrasi dari legacy {@code MRController} + {@code MedicalRecordManagerImpl} +
 * {@code UserManagerImpl.getUnitUser()}:
 * <ul>
 * <li>{@code UserManagerImpl.getUnitUser()} → {@link #getUnits(String)}</li>
 * <li>{@code PatientController.searchPatient()} →
 * {@link #searchPatients(String, String, String, String, String)}</li>
 * <li>{@code MRController.getMrStatusByPatientId()} (input MR code langsung) →
 * {@link #lookupByCode(String)}</li>
 * <li>{@code MRController.requestMR()} →
 * {@link #requestBorrow(Integer, List)}</li>
 * </ul>
 */
@Service
public class MrBorrowRequestService {

    private static final String TERSEDIA = "10";
    private static final String SEDANG_DIPINJAM = "8";
    private static final String AKAN_DIPINJAM = "9";
    private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public MrBorrowRequestService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Daftar unit (LOKASI) tempat user login bertugas.
     * Sama persis dengan legacy {@code UserManagerImpl.getUnitUser()}.
     */
    public List<MrBorrowUnitResponse> getUnits(String username) {
        return jdbcTemplate.query(
                "select unit.n_unit_id, unit.v_unit_code, unit.v_unit_name "
                        + "from ms_staff_in_unit siu "
                        + "join ms_unit unit on unit.n_unit_id = siu.n_unit_id "
                        + "join ms_user usr on usr.n_staff_id = siu.n_staff_id "
                        + "where upper(usr.v_user_name) = ? "
                        + "order by unit.v_unit_name",
                (resultSet, rowNum) -> new MrBorrowUnitResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name")),
                username.toUpperCase());
    }

    /**
     * Sama persis dengan legacy {@code PatientController.searchPatient()}.
     */
    public List<MrBorrowSearchResultResponse> searchPatients(String mrCode, String patientName, String nik,
            String birthDate, String address) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<Object>();

        sql.append("select mr.v_mr_code, mr.v_mr_status, patient.v_patient_name, patient.nik, ")
                .append("patient.d_patient_dob, patient.v_patient_main_addr ")
                .append("from tb_medical_record mr ")
                .append("join ms_patient patient on patient.n_patient_id = mr.n_patient_id ")
                .append("where 1=1 ");

        if (hasText(mrCode)) {
            sql.append("and mr.v_mr_code like ? ");
            args.add(buildLike(normalizeUpper(mrCode)));
        }
        if (hasText(patientName)) {
            sql.append("and upper(patient.v_patient_name) like ? ");
            args.add(buildLike(normalizeUpper(patientName)));
        }
        if (hasText(nik)) {
            sql.append("and patient.nik like ? ");
            args.add(buildLike(nik.trim()));
        }
        if (hasText(address)) {
            sql.append("and upper(patient.v_patient_main_addr) like ? ");
            args.add(buildLike(normalizeUpper(address)));
        }
        if (hasText(birthDate)) {
            sql.append("and patient.d_patient_dob = ? ");
            args.add(Date.valueOf(LocalDate.parse(birthDate, BIRTH_DATE_FORMATTER)));
        }

        sql.append("order by patient.v_patient_name limit 100");

        return jdbcTemplate.query(sql.toString(), args.toArray(), this::mapSearchResult);
    }

    /**
     * Pencarian berkas berdasarkan No. MR yang diketik langsung (raw digit atau
     * format xx-xx-xx).
     * Sama persis dengan legacy {@code MedisafeUtil.convertToMrCode()} +
     * {@code getMedicalRecord()}.
     */
    public MrBorrowSearchResultResponse lookupByCode(String rawCode) {
        String mrCode = normalizeMrCode(rawCode);
        if (mrCode == null) {
            throw new IllegalArgumentException("No. MR tidak valid.");
        }

        try {
            return jdbcTemplate.queryForObject(
                    "select mr.v_mr_code, mr.v_mr_status, patient.v_patient_name, patient.nik, "
                            + "patient.d_patient_dob, patient.v_patient_main_addr "
                            + "from tb_medical_record mr "
                            + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                            + "where mr.v_mr_code = ?",
                    this::mapSearchResult,
                    mrCode);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Berkas Rekam Medis dengan No. MR " + mrCode + " tidak ditemukan.");
        }
    }

    /**
     * Ajukan peminjaman untuk sekumpulan berkas rekam medis.
     * Sama persis dengan legacy {@code MRController.requestMR()}: hanya berkas
     * berstatus
     * TERSEDIA (atau belum pernah dipinjam) yang bisa diajukan; sisanya dilaporkan
     * gagal.
     */
    @Transactional
    public MrBorrowRequestResultResponse requestBorrow(Integer unitId, List<String> mrCodes) {
        if (unitId == null) {
            throw new IllegalArgumentException("LOKASI harus dipilih.");
        }
        if (mrCodes == null || mrCodes.isEmpty()) {
            throw new IllegalArgumentException("Pilih berkas rekam medis yang akan dipinjam.");
        }

        List<String> failedCodes = new ArrayList<String>();
        int requestedCount = 0;

        for (String mrCode : mrCodes) {
            String currentStatus = getMrStatus(mrCode);
            if (currentStatus == null || TERSEDIA.equals(currentStatus)) {
                jdbcTemplate.update(
                        "update tb_medical_record set v_mr_status = ?, n_unit_id = ? where v_mr_code = ?",
                        AKAN_DIPINJAM, unitId, mrCode);
                requestedCount++;
            } else {
                failedCodes.add(mrCode);
            }
        }

        return new MrBorrowRequestResultResponse(requestedCount, failedCodes);
    }

    private String getMrStatus(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select v_mr_status from tb_medical_record where v_mr_code = ?",
                    String.class,
                    mrCode);
        } catch (EmptyResultDataAccessException exception) {
            return SEDANG_DIPINJAM; // berkas tidak ditemukan, perlakukan sebagai tidak bisa dipinjam
        }
    }

    private MrBorrowSearchResultResponse mapSearchResult(java.sql.ResultSet resultSet, int rowNum)
            throws java.sql.SQLException {
        return new MrBorrowSearchResultResponse(
                resultSet.getString("v_mr_code"),
                resultSet.getString("v_patient_name"),
                resultSet.getString("nik"),
                toIsoDate(resultSet.getDate("d_patient_dob")),
                resultSet.getString("v_patient_main_addr"),
                statusLabel(resultSet.getString("v_mr_status")));
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

    /**
     * Sama persis dengan legacy {@code MedisafeUtil.convertToMrCode(String)}:
     * menerima
     * 6 digit angka mentah atau 8 karakter format xx-xx-xx.
     */
    private String normalizeMrCode(String rawCode) {
        if (!hasText(rawCode)) {
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildLike(String value) {
        return "%" + value + "%";
    }

    private String normalizeUpper(String value) {
        return value.trim().toUpperCase();
    }

    private String toIsoDate(Date date) {
        return date != null ? date.toLocalDate().format(BIRTH_DATE_FORMATTER) : null;
    }
}
