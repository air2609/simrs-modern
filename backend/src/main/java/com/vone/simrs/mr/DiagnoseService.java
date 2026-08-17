package com.vone.simrs.mr;

import com.vone.simrs.apotik.ApotikItemOptionResponse;
import com.vone.simrs.apotik.ApotikLineItemRequest;
import com.vone.simrs.apotik.ApotikPatientTypeResponse;
import com.vone.simrs.apotik.ApotikSaveRequest;
import com.vone.simrs.apotik.ApotikSaveResultResponse;
import com.vone.simrs.apotik.ApotikService;
import com.vone.simrs.auth.LegacyAuthService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0206 (FORM REKAM MEDIS DIAGNOSA), tab DIAGNOSA PASIEN
 * &amp; HISTORY DIAGNOSA.
 *
 * <p>
 * Migrasi dari legacy {@code MedicalRecordDiagnose} (UI controller) +
 * {@code DiagnoseManagerImpl} + {@code TbDiagnoseDAO} + {@code IcdManagerImpl}:
 * <ul>
 * <li>{@code DiagnoseManagerImpl.getRegistrationDetail()} →
 * {@link #getRegistration(String)}</li>
 * <li>{@code IcdManagerImpl.serachIcdByCodeAndName()} →
 * {@link #searchIcd(String, String)}</li>
 * <li>{@code MraddItemController.searchItems()} (pencarian obat + PPN rajal) →
 * {@link #searchItems(String, String, boolean)}</li>
 * <li>{@code MedicalRecordDiagnose.save()} +
 * {@code TbDiagnoseDAO.saveDiagnoseAndReceipt()} →
 * {@link #saveDiagnose(DiagnoseSaveRequestBody, String)} (resep &amp; deduksi
 * stok didelegasikan
 * ke {@link ApotikService#createNote} yang sudah mengimplementasikan logika
 * legacy tersebut)</li>
 * <li>{@code DiagnoseManagerImpl.getDiagnoseHistory()} →
 * {@link #getHistory(String)}</li>
 * </ul>
 */
@Service
public class DiagnoseService {

    private static final short DIAGNOSA_RANAP = 11;
    private static final short DIAGNOSA_RAJAL = 12;
    private static final String APTK_UNIT_CODE = "APTK";
    private static final String DEFAULT_TARIFF_CLASS = "KELAS II";

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final ApotikService apotikService;
    private final double pajakObatRajal;

    public DiagnoseService(
            JdbcTemplate jdbcTemplate,
            LegacyAuthService legacyAuthService,
            ApotikService apotikService,
            @Value("${app.pajak.obat-rajal:0.03}") double pajakObatRajal) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.apotikService = apotikService;
        this.pajakObatRajal = pajakObatRajal;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    public List<ApotikPatientTypeResponse> getPatientTypes() {
        return jdbcTemplate.query(
                "select n_patient_type_id, v_tpatient, v_tpatient_desc from ms_patient_type order by v_tpatient",
                (resultSet, rowNum) -> new ApotikPatientTypeResponse(
                        resultSet.getInt("n_patient_type_id"),
                        resultSet.getString("v_tpatient"),
                        resultSet.getString("v_tpatient_desc")));
    }

    /**
     * Sama persis dengan legacy {@code IcdManagerImpl.searchIcd()}.
     */
    public List<DiagnoseIcdOptionResponse> searchIcd(String code, String name) {
        return jdbcTemplate.query(
                "select n_icd_id, v_icd_code, v_icd_name from ms_icd "
                        + "where upper(v_icd_code) like ? and upper(v_icd_name) like ? "
                        + "order by v_icd_name limit 200",
                (resultSet, rowNum) -> new DiagnoseIcdOptionResponse(
                        resultSet.getInt("n_icd_id"),
                        resultSet.getString("v_icd_code"),
                        resultSet.getString("v_icd_name")),
                like(code), like(name));
    }

    /**
     * Sama persis dengan legacy {@code MraddItemController.searchItems()}: cari
     * obat di
     * warehouse APTK, kelas tarif KELAS II. Untuk pasien rawat jalan (rajal), harga
     * ditambah PPN.
     */
    public List<ApotikItemOptionResponse> searchItems(String code, String name, boolean isRajal) {
        List<ApotikItemOptionResponse> items = apotikService.searchItems(findAptkUnitId(), code, name,
                DEFAULT_TARIFF_CLASS);
        if (!isRajal) {
            return items;
        }
        List<ApotikItemOptionResponse> withTax = new ArrayList<ApotikItemOptionResponse>();
        for (ApotikItemOptionResponse item : items) {
            double taxedPrice = item.getPrice() + (item.getPrice() * pajakObatRajal);
            withTax.add(new ApotikItemOptionResponse(
                    item.getItemId(), item.getItemCode(), item.getItemName(), item.getUnitName(),
                    taxedPrice, item.getStockQuantity(), item.getJasaR(), item.getItemType()));
        }
        return withTax;
    }

    /**
     * Sama persis dengan legacy {@code PatientController.searchRegisteredPatient()}
     * (bandbox NO. MR
     * pada {@code newDiagnosa.zul}): cari pasien terdaftar berdasarkan No. MR,
     * nama, tanggal lahir, dan alamat.
     */
    public List<DiagnosePatientSearchResultResponse> searchPatients(String mrCode, String patientName,
            String birthDate, String address) {
        // Sama persis dengan legacy PatientController.searchRegisteredPatient():
        // minimal satu field pencarian wajib diisi.
        if (!hasText(mrCode) && !hasText(patientName) && !hasText(birthDate) && !hasText(address)) {
            throw new IllegalArgumentException("SALAH SATU FIELD HARUS DIISI!");
        }

        // Sama persis dengan legacy PatientController.searchRegisteredPatient():
        // jika No. MR diketik 6 digit, ubah ke format tersimpan XX-XX-XX.
        String searchMrCode = hasText(mrCode) ? normalizeMrCodeForSearch(mrCode) : null;

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        sql.append("select distinct mr.v_mr_code, patient.v_patient_name, patient.d_patient_dob, ")
                .append("patient.v_patient_main_addr ")
                .append("from tb_medical_record mr ")
                .append("join ms_patient patient on patient.n_patient_id = mr.n_patient_id ")
                .append("join tb_registration reg on reg.n_mr_id = mr.n_mr_id ")
                // Sama persis dengan legacy MsPatientDAO.searchPatientRegistered():
                // hanya pasien dengan registrasi aktif (REG_ACTIVE = 1) yang tampil.
                .append("where reg.reg_status = ? ");
        params.add(1);

        if (hasText(searchMrCode)) {
            sql.append("and upper(mr.v_mr_code) like ? ");
            params.add(like(searchMrCode));
        }
        if (hasText(patientName)) {
            sql.append("and upper(patient.v_patient_name) like ? ");
            params.add(like(patientName));
        }
        if (hasText(address)) {
            sql.append("and upper(patient.v_patient_main_addr) like ? ");
            params.add(like(address));
        }
        if (hasText(birthDate)) {
            sql.append("and patient.d_patient_dob = ? ");
            params.add(java.sql.Date.valueOf(LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE)));
        }
        sql.append("order by patient.v_patient_name limit 100");

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (resultSet, rowNum) -> new DiagnosePatientSearchResultResponse(
                        resultSet.getString("v_mr_code"),
                        resultSet.getString("v_patient_name"),
                        toIsoDate(resultSet.getDate("d_patient_dob")),
                        resultSet.getString("v_patient_main_addr")));
    }

    /**
     * Sama persis dengan legacy
     * {@code DiagnoseManagerImpl.getRegistrationDetail()}.
     */
    public DiagnoseRegistrationResponse getRegistration(String rawMrCode) {
        String mrCode = normalizeMrCode(rawMrCode);
        RegistrationRow reg = findLastRegistration(mrCode);
        if (reg == null) {
            throw new IllegalArgumentException("PASIEN BELUM TERDAFTAR!");
        }

        Integer existingDiagnoseId = null;
        String notes = "BB= KG, TB= CM, Tensimeter= / mmHg";
        List<Integer> icdIds = new ArrayList<Integer>();
        List<String> icdNames = new ArrayList<String>();

        try {
            existingDiagnoseId = jdbcTemplate.queryForObject(
                    "select n_diagnose_id from tb_diagnose where n_reg_id = ?", Integer.class, reg.regId);
            notes = jdbcTemplate.queryForObject(
                    "select v_syntom from tb_diagnose where n_diagnose_id = ?", String.class, existingDiagnoseId);
            final Integer diagnoseId = existingDiagnoseId;
            jdbcTemplate.query(
                    "select icdd.n_icd_id, icd.v_icd_name from tb_icd_diagnose icdd "
                            + "join ms_icd icd on icd.n_icd_id = icdd.n_icd_id where icdd.n_diagnose_id = ?",
                    (resultSet, rowNum) -> {
                        icdIds.add(resultSet.getInt("n_icd_id"));
                        icdNames.add(resultSet.getString("v_icd_name"));
                        return null;
                    },
                    diagnoseId);
        } catch (EmptyResultDataAccessException exception) {
            existingDiagnoseId = null;
        }

        return new DiagnoseRegistrationResponse(
                reg.regId, reg.mrCode, reg.patientName, reg.gender, reg.birthDate,
                reg.doctorName, reg.unitLabel, reg.patientTypeId, reg.patientTypeDesc,
                reg.ranap, existingDiagnoseId, notes, icdIds, icdNames);
    }

    /**
     * Sama persis dengan legacy {@code DiagnoseManagerImpl.getDiagnoseHistory()}.
     */
    public List<DiagnoseHistoryItemResponse> getHistory(String rawMrCode) {
        String mrCode = normalizeMrCode(rawMrCode);
        Integer mrId;
        try {
            mrId = jdbcTemplate.queryForObject(
                    "select n_mr_id from tb_medical_record where v_mr_code = ?", Integer.class, mrCode);
        } catch (EmptyResultDataAccessException exception) {
            return new ArrayList<DiagnoseHistoryItemResponse>();
        }

        List<DiagnoseRow> diagnoses = jdbcTemplate.query(
                "select n_diagnose_id, n_reg_id, d_whn_create, v_unit_name, v_doctor_name, v_syntom, v_receipt "
                        + "from tb_diagnose where n_mr_id = ? order by d_whn_create asc",
                (resultSet, rowNum) -> new DiagnoseRow(
                        resultSet.getInt("n_diagnose_id"),
                        getNullableInteger(resultSet, "n_reg_id"),
                        resultSet.getTimestamp("d_whn_create"),
                        resultSet.getString("v_unit_name"),
                        resultSet.getString("v_doctor_name"),
                        resultSet.getString("v_syntom"),
                        resultSet.getString("v_receipt")),
                mrId);

        List<DiagnoseHistoryItemResponse> result = new ArrayList<DiagnoseHistoryItemResponse>();
        for (DiagnoseRow row : diagnoses) {
            String icdNames = String.join(",", jdbcTemplate.query(
                    "select icd.v_icd_name from tb_icd_diagnose icdd "
                            + "join ms_icd icd on icd.n_icd_id = icdd.n_icd_id where icdd.n_diagnose_id = ?",
                    (resultSet, rowNum) -> resultSet.getString("v_icd_name"),
                    row.diagnoseId));

            String labResult = "-";
            if (row.regId != null) {
                String labs = jdbcTemplate.queryForObject(
                        "select string_agg(lab.v_lab_rslt_code, ',') from tb_examination exam "
                                + "join tb_laboratory_result lab on lab.n_exam_id = exam.n_exam_id "
                                + "where exam.n_reg_id = ?",
                        String.class, row.regId);
                if (labs != null && !labs.isEmpty()) {
                    labResult = labs;
                }
            }

            result.add(new DiagnoseHistoryItemResponse(
                    toIsoDateTime(row.createdAt), row.unitName, row.doctorName, row.notes,
                    icdNames.isEmpty() ? "-" : icdNames,
                    labResult,
                    row.receipt != null && !row.receipt.isEmpty() ? row.receipt : "-"));
        }
        return result;
    }

    /**
     * Sama persis dengan legacy {@code MedicalRecordDiagnose.save()} +
     * {@code TbDiagnoseDAO.saveDiagnoseAndReceipt()}. Resep (jika ada) dibuat
     * melalui
     * {@link ApotikService#createNote} sehingga deduksi stok per-batch identik
     * dengan
     * modul Apotik yang sudah teruji.
     */
    @Transactional
    public DiagnoseSaveResultResponse saveDiagnose(DiagnoseSaveRequestBody body, String username) {
        if (!hasText(body.getMrCode())) {
            throw new IllegalArgumentException("NO MR WAJIB DIISI..");
        }
        if (!hasText(body.getNotes())) {
            throw new IllegalArgumentException("KELUHAN PASIEN WAJIB DIISI..");
        }
        if (body.getIcdIds() == null || body.getIcdIds().isEmpty()) {
            throw new IllegalArgumentException("DIAGNOSA WAJIB DIISI..");
        }

        String mrCode = normalizeMrCode(body.getMrCode());
        RegistrationRow reg = findLastRegistration(mrCode);
        if (reg == null) {
            throw new IllegalArgumentException("PASIEN BELUM TERDAFTAR!");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        short diagnoseType = "in".equalsIgnoreCase(body.getDiagnoseType()) ? DIAGNOSA_RANAP : DIAGNOSA_RAJAL;

        Integer diagnoseId;
        if (body.getExistingDiagnoseId() != null) {
            diagnoseId = body.getExistingDiagnoseId();
            jdbcTemplate.update(
                    "update tb_diagnose set v_syntom = ?, n_diagnose_type = ?, v_who_change = ?, d_whn_change = ? "
                            + "where n_diagnose_id = ?",
                    body.getNotes(), diagnoseType, username, now, diagnoseId);
            jdbcTemplate.update("delete from tb_icd_diagnose where n_diagnose_id = ?", diagnoseId);
        } else {
            diagnoseId = getNextSequence("tb_diagnose_n_diagnose_id_seq");
            jdbcTemplate.update(
                    "insert into tb_diagnose (n_diagnose_id, n_mr_id, v_doctor_name, v_unit_name, "
                            + "d_whn_create, v_who_create, n_reg_id, v_syntom, n_diagnose_type) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    diagnoseId, reg.mrId, reg.doctorName, reg.unitLabel, now, username,
                    reg.regId, body.getNotes(), diagnoseType);
        }

        for (Integer icdId : body.getIcdIds()) {
            Integer icdDiagnoseId = getNextSequence("tb_icd_diagnose_n_icd_diagnose_id_seq");
            jdbcTemplate.update(
                    "insert into tb_icd_diagnose (n_icd_diagnose_id, n_diagnose_id, n_icd_id, "
                            + "d_whn_create, v_who_change) values (?, ?, ?, ?, ?)",
                    icdDiagnoseId, diagnoseId, icdId, now, username);
        }

        Integer noteId = null;
        String noteNumber = null;
        if (body.getPrescriptionLines() != null && !body.getPrescriptionLines().isEmpty()) {
            ApotikSaveRequest apotikRequest = new ApotikSaveRequest();
            apotikRequest.setUnitId(findAptkUnitId());
            apotikRequest.setReferencePatient(true);
            apotikRequest.setExistingMrCode(mrCode);
            apotikRequest.setLines(body.getPrescriptionLines());

            ApotikSaveResultResponse noteResult = apotikService.createNote(apotikRequest, username);
            noteId = noteResult.getNoteId();
            noteNumber = noteResult.getNoteNumber();

            jdbcTemplate.update(
                    "update tb_diagnose set v_receipt = ? where n_diagnose_id = ?",
                    buildReceiptText(body.getPrescriptionLines()), diagnoseId);
        }

        jdbcTemplate.update("update tb_registration set antrian_status = ? where n_reg_id = ?", 1, reg.regId);

        return new DiagnoseSaveResultResponse(diagnoseId, noteId, noteNumber);
    }

    private String buildReceiptText(List<ApotikLineItemRequest> lines) {
        StringBuilder sb = new StringBuilder();
        for (ApotikLineItemRequest line : lines) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(hasText(line.getDescription()) ? line.getDescription() : line.getLineType());
            sb.append(" ").append(line.getQuantity());
        }
        return sb.toString();
    }

    private RegistrationRow findLastRegistration(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select reg.n_reg_id, reg.n_transfer_reg_id, mr.n_mr_id, mr.v_mr_code, "
                            + "patient.v_patient_name, patient.v_patient_gender, patient.d_patient_dob, "
                            + "patient.n_patient_type_id, ptype.v_tpatient_desc, "
                            + "staff.v_staff_name, unit.v_unit_name "
                            + "from tb_registration reg "
                            + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                            + "join ms_patient patient on patient.n_patient_id = mr.n_patient_id "
                            + "left join ms_patient_type ptype on ptype.n_patient_type_id = patient.n_patient_type_id "
                            + "left join ms_staff staff on staff.n_staff_id = reg.n_staff_id "
                            + "left join ms_unit unit on unit.n_unit_id = reg.n_unit_id "
                            + "where mr.v_mr_code = ? "
                            + "order by reg.d_registration_date desc limit 1",
                    (resultSet, rowNum) -> new RegistrationRow(
                            resultSet.getInt("n_reg_id"),
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("v_patient_name"),
                            "M".equalsIgnoreCase(resultSet.getString("v_patient_gender")) ? "M" : "F",
                            toIsoDate(resultSet.getDate("d_patient_dob")),
                            resultSet.getString("v_staff_name"),
                            resultSet.getString("v_unit_name") != null
                                    ? resultSet.getString("v_unit_name")
                                    : "RAWAT INAP",
                            getNullableInteger(resultSet, "n_patient_type_id"),
                            resultSet.getString("v_tpatient_desc"),
                            getNullableInteger(resultSet, "n_transfer_reg_id") != null),
                    mrCode);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private Integer findAptkUnitId() {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_unit_id from ms_unit where v_unit_code = ?", Integer.class, APTK_UNIT_CODE);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalStateException("Unit APTK tidak ditemukan.");
        }
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    /**
     * Sama persis dengan legacy {@code MedisafeUtil.convertToMrCode(String)}.
     */
    private String normalizeMrCode(String rawCode) {
        if (!hasText(rawCode)) {
            throw new IllegalArgumentException("No. MR tidak valid.");
        }
        String trimmed = rawCode.trim();
        if (trimmed.length() != 6 && trimmed.length() != 8) {
            throw new IllegalArgumentException("No. MR tidak valid.");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.length() == 8 && (i == 2 || i == 5)) {
                continue;
            }
            char c = trimmed.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("No. MR tidak valid.");
            }
        }
        if (trimmed.length() == 8) {
            return trimmed;
        }
        return trimmed.substring(0, 2) + "-" + trimmed.substring(2, 4) + "-" + trimmed.substring(4, 6);
    }

    /**
     * Sama persis dengan legacy
     * {@code PatientManagerImpl.serachRegisteredPatient()}
     * untuk keperluan query bandbox: jika No. MR diketik 6 digit, konversi ke
     * format tersimpan XX-XX-XX. Jika tidak, kembalikan input apa adanya agar
     * pencarian LIKE parsial tetap berfungsi.
     */
    private String normalizeMrCodeForSearch(String rawCode) {
        if (!hasText(rawCode)) {
            return null;
        }
        String trimmed = rawCode.trim();
        if (trimmed.length() == 6 && trimmed.chars().allMatch(Character::isDigit)) {
            return trimmed.substring(0, 2) + "-" + trimmed.substring(2, 4) + "-" + trimmed.substring(4, 6);
        }
        return trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String like(String value) {
        return "%" + (hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "") + "%";
    }

    private String toIsoDate(java.sql.Date date) {
        return date != null ? date.toLocalDate().toString() : null;
    }

    private String toIsoDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime().toString() : null;
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class RegistrationRow {
        final Integer regId;
        final Integer mrId;
        final String mrCode;
        final String patientName;
        final String gender;
        final String birthDate;
        final String doctorName;
        final String unitLabel;
        final Integer patientTypeId;
        final String patientTypeDesc;
        final boolean ranap;

        RegistrationRow(Integer regId, Integer mrId, String mrCode, String patientName, String gender,
                String birthDate, String doctorName, String unitLabel, Integer patientTypeId,
                String patientTypeDesc, boolean ranap) {
            this.regId = regId;
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientName = patientName;
            this.gender = gender;
            this.birthDate = birthDate;
            this.doctorName = doctorName;
            this.unitLabel = unitLabel;
            this.patientTypeId = patientTypeId;
            this.patientTypeDesc = patientTypeDesc;
            this.ranap = ranap;
        }
    }

    private static final class DiagnoseRow {
        final Integer diagnoseId;
        final Integer regId;
        final Timestamp createdAt;
        final String unitName;
        final String doctorName;
        final String notes;
        final String receipt;

        DiagnoseRow(Integer diagnoseId, Integer regId, Timestamp createdAt, String unitName,
                String doctorName, String notes, String receipt) {
            this.diagnoseId = diagnoseId;
            this.regId = regId;
            this.createdAt = createdAt;
            this.unitName = unitName;
            this.doctorName = doctorName;
            this.notes = notes;
            this.receipt = receipt;
        }
    }
}
