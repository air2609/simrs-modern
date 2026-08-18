package com.vone.simrs.ward;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0031 (TRANSAKSI BANGSAL / ward.zul).
 *
 * <p>
 * Migrasi dari legacy {@code WardTransactionController} +
 * {@code WardTransactionManagerImpl} + {@code WardTransactionDAO} +
 * {@code PatientInventoryController} + {@code PatientInventoryManagerImpl}.
 */
@Service
public class WardService {

    private static final int DOCTOR_GROUP = 4;
    private static final int REG_ACTIVE = 1;
    private static final short NOTE_ACTIVE = 1;
    private static final short NOTE_VALIDATED = 2;
    private static final short NOTE_CANCELED = 0;
    private static final short BELUM_LUNAS = 0;
    private static final String MAIN_DOCTOR = "Y";
    private static final String NO_DOCTOR = "N";
    private static final String DISC_RP = "RP";
    private static final String DISC_PERCENT = "%";
    private static final String LINE_TREATMENT = "TREATMENT";
    private static final String LINE_ITEM = "ITEM";
    private static final String LINE_MISC = "MISC";
    private static final String MISC_CODE = "MISC-001";
    private static final String DEFAULT_TARIFF_CLASS = "KELAS III";
    private static final String NON_PAKET = "NON-PAKET";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public WardService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ------------------------------------------------------------------ masters

    /**
     * Unit lokasi transaksi = unit tempat user bertugas. Migrasi dari
     * {@code UserManagerImpl.getUnitUser()}.
     */
    public WardMastersResponse getMasters(String username) {
        List<WardUnitResponse> units = jdbcTemplate.query(
                "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                        + "from ms_unit unt "
                        + "join ms_staff_in_unit stfunit on stfunit.n_unit_id = unt.n_unit_id "
                        + "join ms_user usr on usr.n_staff_id = stfunit.n_staff_id "
                        + "where upper(usr.v_user_name) = ? order by unt.v_unit_name",
                (resultSet, rowNum) -> new WardUnitResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        getNullableInteger(resultSet, "n_whouse_id")),
                normalizeUsername(username));
        return new WardMastersResponse(units);
    }

    // ------------------------------------------------------------------ pasien ranap

    /**
     * Cari pasien rawat inap yang sedang dirawat. Migrasi dari
     * {@code PatientController.searchRanapPatient()} + {@code MsPatientDAO.searchRanapPatient()}.
     */
    public List<WardPatientOptionResponse> searchRanapPatients(String mrCode, String patientName,
            String address) {
        return jdbcTemplate.query(
                "select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, "
                        + "pat.v_patient_main_addr, pat.n_patient_type_id "
                        + "from ms_patient pat "
                        + "join tb_medical_record mr on mr.n_patient_id = pat.n_patient_id "
                        + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                        + "join tb_bed_occupancy boc on boc.n_reg_primary_id = reg.n_reg_id "
                        + "where reg.reg_status = 1 and boc.d_check_out_time is null "
                        + "and mr.v_mr_code like ? and pat.v_patient_name like ? "
                        + "and pat.v_patient_main_addr like ? limit 100",
                (resultSet, rowNum) -> {
                    Integer typeId = getNullableInteger(resultSet, "n_patient_type_id");
                    return new WardPatientOptionResponse(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("v_patient_name"),
                            typeId != null && typeId == 8 ? "BPJS" : "NON BPJS",
                            resultSet.getString("v_patient_main_addr"));
                },
                like(normalizeOptionalUpper(mrCode)),
                like(normalizeOptionalUpper(patientName)),
                like(normalizeOptionalUpper(address)));
    }

    /**
     * Detail pasien ranap + registrasi aktif + bed occupancy. Migrasi dari
     * {@code WardTransactionManagerImpl.getRegistrationDetil()} +
     * {@code WardTransactionDAO.getRanapByMrCode()}.
     */
    public WardPatientDetailResponse getPatientDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        RanapRow ranap = findRanapByMrCode(normalizeMrCode(mrCode));
        BedRow bed = findBedByRegId(ranap.regId);
        return new WardPatientDetailResponse(
                ranap.mrId, ranap.mrCode, ranap.regId, ranap.regNo, ranap.patientId,
                ranap.patientName, ranap.gender, ranap.birthDate,
                calculateAgeString(ranap.birthDate), ranap.address,
                ranap.patientTypeId, ranap.patientTypeName,
                ranap.doctorId, ranap.doctorName,
                bed == null ? "" : bed.treatmentClass,
                bed == null ? "" : bed.hall,
                bed == null ? "" : bed.bed);
    }

    private RanapRow findRanapByMrCode(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select reg.n_reg_id, reg.v_reg_secondary_id, reg.n_staff_id, mr.n_mr_id, "
                            + "mr.v_mr_code, pat.n_patient_id, pat.v_patient_name, "
                            + "pat.v_patient_gender, pat.d_patient_dob, pat.v_patient_main_addr, "
                            + "pat.n_patient_type_id, pt.v_tpatient_desc, st.v_staff_code, "
                            + "st.v_staff_name "
                            + "from tb_registration reg "
                            + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                            + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                            + "left join ms_patient_type pt on pt.n_patient_type_id = pat.n_patient_type_id "
                            + "left join ms_staff st on st.n_staff_id = reg.n_staff_id "
                            + "where reg.reg_status = ? and reg.v_reg_secondary_id like 'I%' "
                            + "and upper(mr.v_mr_code) = ? "
                            + "order by reg.d_registration_date desc limit 1",
                    (resultSet, rowNum) -> new RanapRow(
                            resultSet.getInt("n_reg_id"),
                            resultSet.getString("v_reg_secondary_id"),
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_gender"),
                            toIsoDate(resultSet.getDate("d_patient_dob")),
                            resultSet.getString("v_patient_main_addr"),
                            getNullableInteger(resultSet, "n_patient_type_id"),
                            resultSet.getString("v_tpatient_desc"),
                            getNullableInteger(resultSet, "n_staff_id"),
                            resultSet.getString("v_staff_code"),
                            resultSet.getString("v_staff_name")),
                    REG_ACTIVE, mrCode);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("MR TIDAK DITEMUKAN / PASIEN BUKAN RAWAT INAP!");
        }
    }

    private BedRow findBedByRegId(Integer regId) {
        List<BedRow> rows = jdbcTemplate.query(
                "select tclass.v_tclass_desc, room.v_room_name, bed.v_bed_desc "
                        + "from tb_bed_occupancy boc "
                        + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                        + "join ms_room room on room.n_room_id = bed.n_room_id "
                        + "left join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                        + "where boc.n_reg_primary_id = ? and boc.d_check_out_time is null "
                        + "order by boc.d_check_in_time desc limit 1",
                (resultSet, rowNum) -> new BedRow(
                        resultSet.getString("v_tclass_desc"),
                        resultSet.getString("v_room_name"),
                        resultSet.getString("v_bed_desc")),
                regId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ------------------------------------------------------------------ pencarian

    public List<WardDoctorOptionResponse> searchDoctors(String code, String name) {
        return jdbcTemplate.query(
                "select st.n_staff_id, st.v_staff_code, st.v_staff_name "
                        + "from ms_doctor dr "
                        + "join ms_staff st on st.n_staff_id = dr.n_staff_id "
                        + "where dr.n_msgroup_id = ? and st.d_staff_fired_date is null "
                        + "and st.v_staff_code like ? and st.v_staff_name like ? "
                        + "order by st.v_staff_name limit 100",
                (resultSet, rowNum) -> new WardDoctorOptionResponse(
                        resultSet.getInt("n_staff_id"),
                        resultSet.getString("v_staff_code"),
                        resultSet.getString("v_staff_name")),
                DOCTOR_GROUP,
                like(normalizeOptionalUpper(code)),
                like(normalizeOptionalUpper(name)));
    }

    public List<WardTreatmentOptionResponse> searchTreatments(String code, String name,
            String tariffClass) {
        String effectiveClass = hasText(tariffClass)
                ? tariffClass.trim().toUpperCase(Locale.ROOT) : DEFAULT_TARIFF_CLASS;
        return jdbcTemplate.query(
                "select tfee.n_treatment_fee_id, treat.n_treatment_id, "
                        + "treat.v_treatment_code, treat.v_treatment_name, "
                        + "tfee.n_trtfee_fee, tfee.n_doctor_fee "
                        + "from ms_treatment_fee tfee "
                        + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                        + "join ms_treatment_class tclass on tclass.n_tclass_id = tfee.n_tclass_id "
                        + "join ms_treatment_group grup on grup.n_tgroup_id = treat.n_tgroup_id "
                        + "where tfee.n_trtfee_fee > 0 "
                        + "and treat.v_treatment_code like ? and treat.v_treatment_name like ? "
                        + "and tclass.v_tclass_desc = ? and grup.v_tgroup_name = ? limit 100",
                (resultSet, rowNum) -> new WardTreatmentOptionResponse(
                        resultSet.getInt("n_treatment_fee_id"),
                        resultSet.getInt("n_treatment_id"),
                        resultSet.getString("v_treatment_code"),
                        resultSet.getString("v_treatment_name"),
                        toDouble(resultSet.getObject("n_trtfee_fee")),
                        toDouble(resultSet.getObject("n_doctor_fee"))),
                like(normalizeOptionalUpper(code)),
                like(normalizeOptionalUpper(name)),
                effectiveClass, NON_PAKET);
    }

    public List<WardItemOptionResponse> searchItems(Integer warehouseId, String code, String name) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Unit tidak memiliki gudang.");
        }
        return jdbcTemplate.query(
                "select inv.n_item_id, item.v_item_code, item.v_item_name, "
                        + "meas.v_mitem_end_quantify, sell.n_selling_price, "
                        + "sum(inv.n_item_inv_qty) as stock "
                        + "from tb_item_inventory inv "
                        + "join ms_item item on item.n_item_id = inv.n_item_id "
                        + "left join ms_item_selling_price sell on sell.n_item_id = item.n_item_id "
                        + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                        + "where inv.n_whouse_id = ? and inv.n_item_inv_qty > 0 "
                        + "and upper(item.v_item_code) like ? and upper(item.v_item_name) like ? "
                        + "group by inv.n_item_id, item.v_item_code, item.v_item_name, "
                        + "meas.v_mitem_end_quantify, sell.n_selling_price "
                        + "order by item.v_item_name limit 200",
                (resultSet, rowNum) -> new WardItemOptionResponse(
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        resultSet.getString("v_item_name"),
                        resultSet.getString("v_mitem_end_quantify"),
                        toDouble(resultSet.getObject("n_selling_price")),
                        toDouble(resultSet.getObject("stock"))),
                warehouseId,
                like(normalizeOptionalUpper(code)),
                like(normalizeOptionalUpper(name)));
    }

    // ------------------------------------------------------------------ nota

    /** Cari nota AKTIF untuk unit. Migrasi dari {@code NoteDAO.searchNote()}. */
    public List<WardNoteSummaryResponse> searchNotes(Integer unitId, String noteNo,
            String patientName) {
        return jdbcTemplate.query(
                "select note.n_exam_id, note.v_note_no, pat.v_patient_name, "
                        + "note.n_exam_status, note.d_whn_create "
                        + "from tb_examination note "
                        + "join ms_patient pat on pat.n_patient_id = note.n_patient_id "
                        + "where note.n_unit_id = ? and note.n_exam_status = ? "
                        + "and note.v_note_no like ? and upper(pat.v_patient_name) like ? "
                        + "order by note.d_whn_create desc limit 100",
                (resultSet, rowNum) -> new WardNoteSummaryResponse(
                        resultSet.getInt("n_exam_id"),
                        resultSet.getString("v_note_no"),
                        resultSet.getString("v_patient_name"),
                        resultSet.getInt("n_exam_status"),
                        getNoteStatusLabel(resultSet.getInt("n_exam_status")),
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create"))),
                unitId, (int) NOTE_ACTIVE,
                like(normalizeOptionalUpper(noteNo)),
                like(normalizeOptionalUpper(patientName)));
    }

    /** Detail nota bangsal + baris. Migrasi dari {@code WardTransactionManagerImpl.getNoteDetil()}. */
    public WardNoteDetailResponse getNoteDetail(Integer noteId) {
        NoteHeader header = findNoteHeader(noteId);
        RanapRow ranap = header.regId == null ? null : findRanapByRegId(header.regId);
        BedRow bed = header.regId == null ? null : findBedByRegId(header.regId);
        List<WardNoteLineResponse> lines = getNoteLines(noteId);

        int status = header.status;
        boolean validated = status == NOTE_VALIDATED;
        boolean canceled = status == NOTE_CANCELED;
        return new WardNoteDetailResponse(
                header.noteId, header.noteNo, status, getNoteStatusLabel(status),
                header.total, header.unitId, header.unitName, header.patientId,
                ranap == null ? "" : ranap.mrCode,
                ranap == null ? "" : ranap.patientName,
                ranap == null ? "" : ranap.gender,
                ranap == null ? "" : ranap.birthDate,
                ranap == null ? "" : calculateAgeString(ranap.birthDate),
                ranap == null ? "" : ranap.address,
                ranap == null ? null : ranap.patientTypeId,
                ranap == null ? "" : ranap.patientTypeName,
                header.regId, header.regNo,
                header.doctorId,
                header.doctorCode != null && header.doctorName != null
                        ? header.doctorCode + "-" + header.doctorName : header.doctorName,
                bed == null ? "" : bed.treatmentClass,
                bed == null ? "" : bed.hall,
                bed == null ? "" : bed.bed,
                !validated && !canceled, !validated && !canceled, !canceled, lines);
    }

    private RanapRow findRanapByRegId(Integer regId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select reg.n_reg_id, reg.v_reg_secondary_id, reg.n_staff_id, mr.n_mr_id, "
                            + "mr.v_mr_code, pat.n_patient_id, pat.v_patient_name, "
                            + "pat.v_patient_gender, pat.d_patient_dob, pat.v_patient_main_addr, "
                            + "pat.n_patient_type_id, pt.v_tpatient_desc, st.v_staff_code, "
                            + "st.v_staff_name "
                            + "from tb_registration reg "
                            + "join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                            + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                            + "left join ms_patient_type pt on pt.n_patient_type_id = pat.n_patient_type_id "
                            + "left join ms_staff st on st.n_staff_id = reg.n_staff_id "
                            + "where reg.n_reg_id = ?",
                    (resultSet, rowNum) -> new RanapRow(
                            resultSet.getInt("n_reg_id"),
                            resultSet.getString("v_reg_secondary_id"),
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_gender"),
                            toIsoDate(resultSet.getDate("d_patient_dob")),
                            resultSet.getString("v_patient_main_addr"),
                            getNullableInteger(resultSet, "n_patient_type_id"),
                            resultSet.getString("v_tpatient_desc"),
                            getNullableInteger(resultSet, "n_staff_id"),
                            resultSet.getString("v_staff_code"),
                            resultSet.getString("v_staff_name")),
                    regId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private NoteHeader findNoteHeader(Integer noteId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select note.n_exam_id, note.v_note_no, note.n_exam_status, "
                            + "note.n_total_amount, note.n_unit_id, note.n_patient_id, "
                            + "note.n_reg_id, unit.v_unit_name, reg.v_reg_secondary_id, "
                            + "reg.n_staff_id as doctor_id, st.v_staff_code, st.v_staff_name "
                            + "from tb_examination note "
                            + "left join tb_registration reg on reg.n_reg_id = note.n_reg_id "
                            + "left join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                            + "left join ms_staff st on st.n_staff_id = reg.n_staff_id "
                            + "where note.n_exam_id = ?",
                    (resultSet, rowNum) -> new NoteHeader(
                            resultSet.getInt("n_exam_id"),
                            resultSet.getString("v_note_no"),
                            resultSet.getInt("n_exam_status"),
                            toDouble(resultSet.getObject("n_total_amount")),
                            getNullableInteger(resultSet, "n_unit_id"),
                            resultSet.getString("v_unit_name"),
                            getNullableInteger(resultSet, "n_patient_id"),
                            getNullableInteger(resultSet, "n_reg_id"),
                            resultSet.getString("v_reg_secondary_id"),
                            getNullableInteger(resultSet, "doctor_id"),
                            resultSet.getString("v_staff_code"),
                            resultSet.getString("v_staff_name")),
                    noteId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Nota tidak ditemukan.");
        }
    }

    private List<WardNoteLineResponse> getNoteLines(Integer noteId) {
        List<WardNoteLineResponse> lines = new ArrayList<>();
        lines.addAll(jdbcTemplate.query(
                "select trx.n_treatment_fee_id, treat.v_treatment_code, "
                        + "treat.v_treatment_name, trx.n_qty, trx.n_amount_trx, "
                        + "trx.n_disc_amount, trx.v_disc_type, trx.n_amount_after_disc, "
                        + "trx.n_doctor_id, tfee.n_doctor_fee, st.v_staff_name "
                        + "from tb_treatment_trx trx "
                        + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                        + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                        + "left join ms_staff st on st.n_staff_id = trx.n_doctor_id "
                        + "where trx.n_note_id = ? order by trx.n_treatment_id",
                (resultSet, rowNum) -> {
                    double qty = nvlDouble(resultSet.getObject("n_qty"), 1);
                    double amountTrx = nvlDouble(resultSet.getObject("n_amount_trx"), 0);
                    String name = resultSet.getString("v_treatment_name");
                    double doctorFee = nvlDouble(resultSet.getObject("n_doctor_fee"), 0);
                    String doctorName = resultSet.getString("v_staff_name");
                    if (resultSet.getObject("n_doctor_id") != null && doctorFee > 0
                            && doctorName != null) {
                        name = name + "-" + doctorName;
                    }
                    return new WardNoteLineResponse(
                            LINE_TREATMENT,
                            resultSet.getInt("n_treatment_fee_id"),
                            resultSet.getString("v_treatment_code"), name, qty, "-",
                            qty == 0 ? 0 : amountTrx / qty,
                            resultSet.getString("v_disc_type"),
                            toDouble(resultSet.getObject("n_disc_amount")),
                            toDouble(resultSet.getObject("n_amount_after_disc")),
                            getNullableInteger(resultSet, "n_doctor_id"));
                },
                noteId));
        lines.addAll(jdbcTemplate.query(
                "select trx.n_item_id, item.v_item_code, item.v_item_name, "
                        + "meas.v_mitem_end_quantify, sum(trx.n_qty) as qty, "
                        + "sum(trx.n_amount_trx) as value, sum(trx.n_disc_amount) as discount, "
                        + "trx.v_disc_type, sum(trx.n_amount_after_disc) as total "
                        + "from tb_item_trx trx "
                        + "join ms_item item on item.n_item_id = trx.n_item_id "
                        + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                        + "where trx.n_note_id = ? "
                        + "group by trx.n_item_id, item.v_item_code, item.v_item_name, "
                        + "meas.v_mitem_end_quantify, trx.v_disc_type order by item.v_item_name",
                (resultSet, rowNum) -> {
                    double qty = nvlDouble(resultSet.getObject("qty"), 0);
                    double value = nvlDouble(resultSet.getObject("value"), 0);
                    return new WardNoteLineResponse(
                            LINE_ITEM,
                            resultSet.getInt("n_item_id"),
                            resultSet.getString("v_item_code"),
                            resultSet.getString("v_item_name"),
                            qty, resultSet.getString("v_mitem_end_quantify"),
                            qty == 0 ? 0 : value / qty,
                            resultSet.getString("v_disc_type"),
                            toDouble(resultSet.getObject("discount")),
                            toDouble(resultSet.getObject("total")),
                            null);
                },
                noteId));
        lines.addAll(jdbcTemplate.query(
                "select n_qty, v_misc_name, n_amount_trx, n_disc_amount, v_disc_type, "
                        + "n_amount_after_disc from tb_misc_trx where n_note_id = ? order by n_misc_trx_id",
                (resultSet, rowNum) -> {
                    double qty = nvlDouble(resultSet.getObject("n_qty"), 1);
                    double amountTrx = nvlDouble(resultSet.getObject("n_amount_trx"), 0);
                    return new WardNoteLineResponse(
                            LINE_MISC, null, MISC_CODE,
                            resultSet.getString("v_misc_name"),
                            qty, "-", qty == 0 ? 0 : amountTrx / qty,
                            resultSet.getString("v_disc_type"),
                            toDouble(resultSet.getObject("n_disc_amount")),
                            toDouble(resultSet.getObject("n_amount_after_disc")),
                            null);
                },
                noteId));
        return lines;
    }

    // ------------------------------------------------------------------ simpan / ubah / validasi / batal

    /** Simpan nota bangsal (nota rawat inap, nomor "I-..."). */
    @Transactional
    public WardActionResultResponse saveNote(WardNoteSaveRequest request, String username) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("TIDAK ADA TRANSAKSI YANG AKAN DISIMPAN!");
        }
        RanapRow ranap = findRanapByMrCode(normalizeMrCode(request.getMrCode()));
        UnitRow unit = findUnit(request.getUnitId());
        Timestamp now = new Timestamp(System.currentTimeMillis());

        double total = calculateTotal(request.getLines());
        Integer noteId = getNextSequence("tb_examination_n_exam_id_seq");
        String noteNo = generateRanapNoteNumber(noteId, now, unit.code);

        jdbcTemplate.update(
                "insert into tb_examination (n_exam_id, v_note_no, n_exam_status, n_payment_status, "
                        + "n_unit_id, n_patient_id, n_reg_id, n_total_amount, "
                        + "d_whn_create, v_who_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                noteId, noteNo, (int) NOTE_ACTIVE, (int) BELUM_LUNAS,
                unit.unitId, ranap.patientId, ranap.regId, total, now, username);

        saveNoteLines(noteId, request.getLines(), unit, username, now);
        updateRegistrationDoctor(ranap.regId, request.getDoctorId(), username, now);
        return new WardActionResultResponse(true, "Nota berhasil disimpan.", noteId, noteNo);
    }

    /** Ubah nota bangsal: restore stok, hapus baris, simpan ulang. */
    @Transactional
    public WardActionResultResponse updateNote(Integer noteId, WardNoteSaveRequest request,
            String username) {
        NoteHeader header = findNoteHeader(noteId);
        if (header.status != NOTE_ACTIVE) {
            throw new IllegalStateException("Hanya nota AKTIF yang bisa diubah.");
        }
        UnitRow unit = findUnit(header.unitId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        restoreNoteInventory(noteId, unit.warehouseId);
        deleteNoteLines(noteId);

        double total = calculateTotal(request.getLines());
        jdbcTemplate.update(
                "update tb_examination set n_total_amount = ?, d_whn_change = ?, v_who_change = ? "
                        + "where n_exam_id = ?",
                total, now, username, noteId);
        saveNoteLines(noteId, request.getLines(), unit, username, now);
        if (header.regId != null) {
            updateRegistrationDoctor(header.regId, request.getDoctorId(), username, now);
        }
        return new WardActionResultResponse(true, "Nota berhasil diubah.", noteId, header.noteNo);
    }

    private void saveNoteLines(Integer noteId, List<WardLineRequest> lines, UnitRow unit,
            String username, Timestamp now) {
        for (WardLineRequest line : lines) {
            if (LINE_TREATMENT.equals(line.getLineType())) {
                saveTreatmentLine(noteId, line, username, now);
            } else if (LINE_ITEM.equals(line.getLineType())) {
                saveItemLine(noteId, line, unit, username, now);
            } else if (LINE_MISC.equals(line.getLineType())) {
                saveMiscLine(noteId, line, username, now);
            }
        }
    }

    private void saveTreatmentLine(Integer noteId, WardLineRequest line, String username,
            Timestamp now) {
        double qty = line.getQty() == null ? 1 : line.getQty();
        double price = line.getPrice() == null ? 0 : line.getPrice();
        double amountBefore = qty * price;
        double discAmount = calculateDiscount(amountBefore, line.getDiscType(), line.getDiscAmount());
        jdbcTemplate.update(
                "insert into tb_treatment_trx (n_note_id, n_treatment_fee_id, n_qty, "
                        + "n_amount_trx, n_disc_amount, v_disc_type, n_amount_after_disc, "
                        + "n_doctor_id, d_whn_create, v_who_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                noteId, line.getReferenceId(), (short) qty, amountBefore, discAmount,
                normalizeDiscountType(line.getDiscType()), amountBefore - discAmount,
                line.getDoctorId(), now, username);
    }

    private void saveItemLine(Integer noteId, WardLineRequest line, UnitRow unit, String username,
            Timestamp now) {
        if (unit.warehouseId == null) {
            throw new IllegalStateException("Unit tidak memiliki gudang.");
        }
        double qty = line.getQty() == null ? 0 : line.getQty();
        double price = line.getPrice() == null ? 0 : line.getPrice();
        double amountBefore = qty * price;
        double discAmount = calculateDiscount(amountBefore, line.getDiscType(), line.getDiscAmount());
        String discType = normalizeDiscountType(line.getDiscType());
        double perUnitHarga = qty == 0 ? 0 : amountBefore / qty;
        double perUnitDisc = qty == 0 ? 0 : discAmount / qty;
        double perUnitAfter = qty == 0 ? 0 : (amountBefore - discAmount) / qty;

        double remaining = qty;
        List<InventoryRow> inventories = findItemInventories(unit.warehouseId, line.getReferenceId());
        for (InventoryRow inv : inventories) {
            if (remaining <= 0) {
                break;
            }
            double picked = Math.min(remaining, inv.qty);
            jdbcTemplate.update(
                    "insert into tb_item_trx (n_note_id, n_item_id, n_batch_id, n_qty, "
                            + "n_amount_trx, n_disc_amount, v_disc_type, n_amount_after_disc, "
                            + "d_whn_create, v_who_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    noteId, line.getReferenceId(), inv.batchId, picked,
                    picked * perUnitHarga, picked * perUnitDisc, discType, picked * perUnitAfter,
                    now, username);
            deductInventory(line.getReferenceId(), inv.batchId, unit.warehouseId, picked);
            remaining -= picked;
        }
        if (remaining > 0) {
            throw new IllegalStateException(
                    "Stok item id " + line.getReferenceId() + " tidak mencukupi.");
        }
    }

    private void saveMiscLine(Integer noteId, WardLineRequest line, String username, Timestamp now) {
        double qty = line.getQty() == null ? 1 : line.getQty();
        double price = line.getPrice() == null ? 0 : line.getPrice();
        double amountBefore = qty * price;
        double discAmount = calculateDiscount(amountBefore, line.getDiscType(), line.getDiscAmount());
        jdbcTemplate.update(
                "insert into tb_misc_trx (n_note_id, v_misc_name, n_qty, n_item_price, "
                        + "n_amount_trx, n_disc_amount, v_disc_type, n_amount_after_disc, "
                        + "d_whn_create, v_who_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                noteId, line.getMiscName() == null ? "BIAYA LAIN-LAIN" : line.getMiscName(),
                (short) qty, (float) price, amountBefore, discAmount,
                normalizeDiscountType(line.getDiscType()), amountBefore - discAmount, now, username);
    }

    private void updateRegistrationDoctor(Integer regId, Integer doctorId, String username,
            Timestamp now) {
        jdbcTemplate.update(
                "update tb_registration set n_staff_id = ?, v_main_doctor_status = ?, "
                        + "v_who_change = ?, d_whn_change = ? where n_reg_id = ?",
                doctorId, doctorId != null ? MAIN_DOCTOR : NO_DOCTOR, username, now, regId);
    }

    /** Set dokter utama (tombol SET DOKTER UTAMA). Migrasi dari {@code setMainDoctor()}. */
    @Transactional
    public WardActionResultResponse setMainDoctor(Integer regId, Integer doctorId, String username) {
        if (regId == null) {
            throw new IllegalArgumentException("registration.is.null");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        updateRegistrationDoctor(regId, doctorId, username, now);
        return new WardActionResultResponse(true, "Dokter utama berhasil diubah.", null, null);
    }

    private List<InventoryRow> findItemInventories(Integer warehouseId, Integer itemId) {
        return jdbcTemplate.query(
                "select n_item_id, n_batch_id, n_item_inv_qty as qty from tb_item_inventory "
                        + "where n_whouse_id = ? and n_item_id = ? and n_item_inv_qty > 0 "
                        + "order by n_item_inv_qty",
                (resultSet, rowNum) -> new InventoryRow(
                        resultSet.getInt("n_item_id"),
                        resultSet.getInt("n_batch_id"),
                        toDouble(resultSet.getObject("qty"))),
                warehouseId, itemId);
    }

    private void deductInventory(Integer itemId, Integer batchId, Integer warehouseId, double qty) {
        jdbcTemplate.update(
                "update tb_item_inventory set n_item_inv_qty = n_item_inv_qty - ? "
                        + "where n_item_id = ? and n_batch_id = ? and n_whouse_id = ?",
                qty, itemId, batchId, warehouseId);
    }

    private void restoreInventory(Integer itemId, Integer batchId, Integer warehouseId, double qty) {
        jdbcTemplate.update(
                "update tb_item_inventory set n_item_inv_qty = n_item_inv_qty + ? "
                        + "where n_item_id = ? and n_batch_id = ? and n_whouse_id = ?",
                qty, itemId, batchId, warehouseId);
    }

    private void restoreNoteInventory(Integer noteId, Integer warehouseId) {
        if (warehouseId == null) {
            return;
        }
        List<InventoryRow> rows = jdbcTemplate.query(
                "select n_item_id, n_batch_id, n_qty from tb_item_trx where n_note_id = ?",
                (resultSet, rowNum) -> new InventoryRow(
                        resultSet.getInt("n_item_id"),
                        resultSet.getInt("n_batch_id"),
                        toDouble(resultSet.getObject("n_qty"))),
                noteId);
        for (InventoryRow row : rows) {
            restoreInventory(row.itemId, row.batchId, warehouseId, row.qty);
        }
    }

    private void deleteNoteLines(Integer noteId) {
        jdbcTemplate.update("delete from tb_treatment_trx where n_note_id = ?", noteId);
        jdbcTemplate.update("delete from tb_item_trx where n_note_id = ?", noteId);
        jdbcTemplate.update("delete from tb_misc_trx where n_note_id = ?", noteId);
    }

    /** Validasi nota + journal. Migrasi dari {@code validate()} + {@code NoteDAO.save()}. */
    @Transactional
    public WardActionResultResponse validateNote(Integer noteId, String username) {
        NoteHeader header = findNoteHeader(noteId);
        if (header.status != NOTE_ACTIVE) {
            throw new IllegalStateException("Hanya nota AKTIF yang bisa divalidasi.");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
                "update tb_examination set n_exam_status = ?, d_whn_change = ?, "
                        + "v_who_change = ?, d_validation_date = ? where n_exam_id = ?",
                (int) NOTE_VALIDATED, now, username, now, noteId);
        createNoteJournal(noteId, header, now, username);
        return new WardActionResultResponse(true, "Nota berhasil divalidasi.", noteId, header.noteNo);
    }

    /** Batalkan nota + restore stok. Migrasi dari {@code NoteDAO.cancelNote()}. */
    @Transactional
    public WardActionResultResponse cancelNote(Integer noteId, String reason, String username) {
        NoteHeader header = findNoteHeader(noteId);
        if (header.status == NOTE_CANCELED) {
            throw new IllegalStateException("Nota sudah dibatalkan.");
        }
        UnitRow unit = findUnit(header.unitId);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        restoreNoteInventory(noteId, unit.warehouseId);
        jdbcTemplate.update(
                "update tb_examination set n_exam_status = ?, v_cancelation_note = ?, "
                        + "d_whn_change = ?, v_who_change = ? where n_exam_id = ?",
                (int) NOTE_CANCELED, reason, now, username, noteId);
        return new WardActionResultResponse(true, "Nota berhasil dibatalkan.", noteId, header.noteNo);
    }

    private void createNoteJournal(Integer noteId, NoteHeader header, Timestamp now, String username) {
        String batchId = "AR" + String.format("%015d", getNextSequence("sq_journal_trx"));
        String voucherNo = header.noteNo;
        Integer coaArId = findCoaIdByGimKey("COA_INPATIENT_AR");
        if (coaArId == null) {
            throw new IllegalStateException("COA AR belum dikonfigurasi.");
        }
        Integer coaMiscId = findCoaIdByGimKey("COA_MISC_TRX");
        UnitRow unit = findUnit(header.unitId);
        Integer coaInvId = unit.warehouseId == null ? null : findWarehouseCoaId(unit.warehouseId);

        List<TreatmentJournalRow> treatments = jdbcTemplate.query(
                "select tfee.n_coa, trx.n_qty, trx.n_disc_amount, trx.n_amount_after_disc, "
                        + "treat.v_treatment_code "
                        + "from tb_treatment_trx trx "
                        + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                        + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                        + "where trx.n_note_id = ?",
                (resultSet, rowNum) -> new TreatmentJournalRow(
                        getNullableInteger(resultSet, "n_coa"),
                        resultSet.getString("v_treatment_code"),
                        nvlDouble(resultSet.getObject("n_qty"), 1),
                        nvlDouble(resultSet.getObject("n_disc_amount"), 0),
                        nvlDouble(resultSet.getObject("n_amount_after_disc"), 0)),
                noteId);
        for (TreatmentJournalRow row : treatments) {
            if (row.coaId == null) {
                throw new IllegalStateException("trx.treatment.coa.null");
            }
            String memo = "TCODE:" + row.code + ";QTY:" + row.qty + ";DISCOUNT:" + row.discAmount;
            insertJournal(batchId, voucherNo, memo, row.amountAfterDisc, 0, now, username, coaArId);
            insertJournal(batchId, voucherNo, memo, 0, row.amountAfterDisc, now, username, row.coaId);
        }

        List<ItemJournalRow> items = jdbcTemplate.query(
                "select trx.n_item_id, item.v_item_code, trx.n_qty, trx.n_disc_amount, "
                        + "trx.n_amount_after_disc, item.n_item_sell_acc_no, item.n_item_cogs_no, "
                        + "batch.n_cogs_price "
                        + "from tb_item_trx trx "
                        + "join ms_item item on item.n_item_id = trx.n_item_id "
                        + "join tb_batch_item batch on batch.n_batch_id = trx.n_batch_id "
                        + "where trx.n_note_id = ?",
                (resultSet, rowNum) -> new ItemJournalRow(
                        resultSet.getInt("n_item_id"),
                        resultSet.getString("v_item_code"),
                        nvlDouble(resultSet.getObject("n_qty"), 0),
                        nvlDouble(resultSet.getObject("n_disc_amount"), 0),
                        nvlDouble(resultSet.getObject("n_amount_after_disc"), 0),
                        getNullableInteger(resultSet, "n_item_sell_acc_no"),
                        getNullableInteger(resultSet, "n_item_cogs_no"),
                        toDouble(resultSet.getObject("n_cogs_price"))),
                noteId);
        for (ItemJournalRow row : items) {
            if (coaInvId == null) {
                throw new IllegalStateException("trx.coa.inventory.unit.not.found");
            }
            if (row.sellCoaId == null) {
                throw new IllegalStateException("trx.item.sell.coa.null");
            }
            if (row.cogsCoaId == null) {
                throw new IllegalStateException("trx.item.cogs.coa.null");
            }
            double cogs = (row.cogsPrice == null ? 0 : row.cogsPrice) * row.qty;
            String memo = "ITEMCODE:" + row.code + ";QTY:" + row.qty + ";DISCOUNT:" + row.discAmount;
            insertJournal(batchId, voucherNo, memo, row.amountAfterDisc, 0, now, username, coaArId);
            insertJournal(batchId, voucherNo, memo, 0, row.amountAfterDisc, now, username, row.sellCoaId);
            insertJournal(batchId, voucherNo, memo, 0, cogs, now, username, coaInvId);
            insertJournal(batchId, voucherNo, memo, cogs, 0, now, username, row.cogsCoaId);
        }

        if (coaMiscId == null) {
            throw new IllegalStateException("trx.misc.coa.null");
        }
        List<MiscJournalRow> miscs = jdbcTemplate.query(
                "select v_misc_name, n_qty, n_amount_after_disc from tb_misc_trx where n_note_id = ?",
                (resultSet, rowNum) -> new MiscJournalRow(
                        resultSet.getString("v_misc_name"),
                        nvlDouble(resultSet.getObject("n_qty"), 1),
                        nvlDouble(resultSet.getObject("n_amount_after_disc"), 0)),
                noteId);
        for (MiscJournalRow row : miscs) {
            String memo = "MISC:" + row.name + ";QTY:" + row.qty;
            insertJournal(batchId, voucherNo, memo, row.amountAfterDisc, 0, now, username, coaArId);
            insertJournal(batchId, voucherNo, memo, 0, row.amountAfterDisc, now, username, coaMiscId);
        }
    }

    private void insertJournal(String batchId, String voucherNo, String desc, double debit,
            double credit, Timestamp now, String username, Integer coaId) {
        Integer journalId = getNextSequence("tb_journal_trx_n_journal_id_seq");
        jdbcTemplate.update(
                "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, "
                        + "v_desc, n_debit, n_credit, d_whn_create, v_who_create, d_apl_date, n_coa_id) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                journalId, batchId, voucherNo, desc, debit, credit, now, username, now, coaId);
    }

    private Integer findCoaIdByGimKey(String gimKey) {
        try {
            return jdbcTemplate.queryForObject(
                    "select coa.n_coa_id from ms_gim gim "
                            + "join ms_coa coa on coa.v_acct_no = gim.v_value where gim.v_key = ?",
                    Integer.class, gimKey);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Integer findWarehouseCoaId(Integer warehouseId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_coa_id from ms_warehouse where n_whouse_id = ?",
                    Integer.class, warehouseId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ inventory pasien

    /** Data modal INVENTORY PASIEN. Migrasi dari PatientInventoryManagerImpl. */
    public WardPatientInventoryResponse getPatientInventory(Integer registrationId) {
        if (registrationId == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        RanapRow ranap = findRanapByRegId(registrationId);
        if (ranap == null) {
            throw new IllegalArgumentException("Registrasi tidak ditemukan.");
        }
        List<WardPatientInventoryItemResponse> items = jdbcTemplate.query(
                "select inv.n_item_id, item.v_item_code, item.v_item_name, "
                        + "meas.v_mitem_end_quantify, "
                        + "coalesce(sum(inv.n_qty), 0) as total_in, "
                        + "coalesce(sum(inv.n_qty_out), 0) as total_out "
                        + "from tb_patient_inventory inv "
                        + "join ms_item item on item.n_item_id = inv.n_item_id "
                        + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                        + "where inv.n_reg_id = ? "
                        + "group by inv.n_item_id, item.v_item_code, item.v_item_name, "
                        + "meas.v_mitem_end_quantify order by item.v_item_name",
                (resultSet, rowNum) -> {
                    int totalIn = getNullableInteger(resultSet, "total_in");
                    int totalOut = getNullableInteger(resultSet, "total_out");
                    return new WardPatientInventoryItemResponse(
                            resultSet.getInt("n_item_id"),
                            resultSet.getString("v_item_code"),
                            resultSet.getString("v_item_name"),
                            resultSet.getString("v_mitem_end_quantify"),
                            totalIn, totalOut, totalIn - totalOut);
                },
                registrationId);
        List<WardPatientInventoryHistoryResponse> history = jdbcTemplate.query(
                "select inv.n_pi_id, inv.n_item_id, inv.d_whn_create, inv.n_qty, inv.n_qty_out "
                        + "from tb_patient_inventory inv where inv.n_reg_id = ? "
                        + "order by inv.n_item_id, inv.d_whn_create",
                (resultSet, rowNum) -> new WardPatientInventoryHistoryResponse(
                        resultSet.getInt("n_pi_id"),
                        resultSet.getInt("n_item_id"),
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create")),
                        getNullableInteger(resultSet, "n_qty"),
                        getNullableInteger(resultSet, "n_qty_out"),
                        resultSet.getInt("n_qty") - resultSet.getInt("n_qty_out")),
                registrationId);
        return new WardPatientInventoryResponse(
                ranap.mrCode, ranap.patientName, registrationId, ranap.regNo, items, history);
    }

    /** Simpan pemakaian inventory pasien (n_qty_out). Migrasi dari PatientInventoryController.save(). */
    @Transactional
    public WardActionResultResponse savePatientInventory(WardPatientInventorySaveRequest request,
            String username) {
        if (request.getRegistrationId() == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        RanapRow ranap = findRanapByRegId(request.getRegistrationId());
        Timestamp now = new Timestamp(System.currentTimeMillis());
        int saved = 0;
        if (request.getLines() != null) {
            for (WardPatientInventoryLineRequest line : request.getLines()) {
                if (line.getQtyOut() == null || line.getQtyOut() == 0) {
                    continue;
                }
                if (line.getQtyOut() < 0) {
                    throw new IllegalArgumentException("common.input.negatif.notallowed");
                }
                Integer sisa = findPatientInventorySisa(request.getRegistrationId(), line.getItemId());
                if (sisa == null || sisa < line.getQtyOut()) {
                    throw new IllegalArgumentException("patient.inventory.input.not.valid");
                }
                jdbcTemplate.update(
                        "insert into tb_patient_inventory (n_item_id, n_pat_id, n_reg_id, n_qty_out, "
                                + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?)",
                        line.getItemId(), ranap.patientId, request.getRegistrationId(),
                        line.getQtyOut(), username, now);
                saved++;
            }
        }
        return new WardActionResultResponse(true,
                saved > 0 ? "Inventory pasien berhasil disimpan." : "Tidak ada pemakaian disimpan.",
                null, null);
    }

    private Integer findPatientInventorySisa(Integer regId, Integer itemId) {
        List<Integer> rows = jdbcTemplate.query(
                "select coalesce(sum(n_qty), 0) - coalesce(sum(n_qty_out), 0) as sisa "
                        + "from tb_patient_inventory where n_reg_id = ? and n_item_id = ?",
                (resultSet, rowNum) -> resultSet.getInt("sisa"),
                regId, itemId);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    /** Hapus entri inventory pasien. */
    @Transactional
    public WardActionResultResponse deletePatientInventory(Integer piId) {
        jdbcTemplate.update("delete from tb_patient_inventory where n_pi_id = ?", piId);
        return new WardActionResultResponse(true, "Riwayat inventory berhasil dihapus.", null, null);
    }

    // ------------------------------------------------------------------ history

    /** Riwayat transaksi pasien (tab HISTORY). Migrasi dari CommonHistoryDAO.getPatientNote(). */
    public WardHistoryResponse getHistory(String mrCode, String mode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        RanapRow ranap = findRanapByMrCode(normalizeMrCode(mrCode));
        boolean global = "global".equalsIgnoreCase(mode);

        List<WardHistoryNoteResponse> notes = jdbcTemplate.query(
                "select note.n_exam_id, note.v_note_no, note.n_exam_status, note.n_total_amount, "
                        + "note.d_whn_create, unit.v_unit_name "
                        + "from tb_examination note "
                        + "left join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                        + "where note.n_patient_id = ? and note.n_exam_status <> ? "
                        + "order by note.d_whn_create desc limit 100",
                (resultSet, rowNum) -> new WardHistoryNoteResponse(
                        resultSet.getInt("n_exam_id"),
                        resultSet.getString("v_note_no"),
                        resultSet.getString("v_unit_name"),
                        resultSet.getInt("n_exam_status"),
                        getNoteStatusLabel(resultSet.getInt("n_exam_status")),
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create")),
                        nvlDouble(resultSet.getObject("n_total_amount"), 0),
                        getHistoryLines(resultSet.getInt("n_exam_id"))),
                ranap.patientId, (int) NOTE_CANCELED);

        double grandTotal = 0;
        for (WardHistoryNoteResponse note : notes) {
            grandTotal += note.getTotal();
        }
        return new WardHistoryResponse(ranap.mrCode, ranap.patientName,
                global ? "GLOBAL" : "PER DIVISI", grandTotal, notes);
    }

    private List<WardHistoryLineResponse> getHistoryLines(Integer noteId) {
        List<WardHistoryLineResponse> lines = new ArrayList<>();
        lines.addAll(jdbcTemplate.query(
                "select trx.n_qty, trx.n_amount_after_disc, trx.d_whn_create, "
                        + "treat.v_treatment_name, unit.v_unit_name "
                        + "from tb_treatment_trx trx "
                        + "join tb_examination note on note.n_exam_id = trx.n_note_id "
                        + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                        + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                        + "left join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                        + "where trx.n_note_id = ?",
                (resultSet, rowNum) -> new WardHistoryLineResponse(
                        resultSet.getString("v_treatment_name"),
                        resultSet.getString("v_unit_name"),
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create")),
                        nvlDouble(resultSet.getObject("n_amount_after_disc"), 0)),
                noteId));
        lines.addAll(jdbcTemplate.query(
                "select trx.n_qty, trx.n_amount_after_disc, trx.d_whn_create, "
                        + "item.v_item_name, unit.v_unit_name "
                        + "from tb_item_trx trx "
                        + "join tb_examination note on note.n_exam_id = trx.n_note_id "
                        + "join ms_item item on item.n_item_id = trx.n_item_id "
                        + "left join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                        + "where trx.n_note_id = ?",
                (resultSet, rowNum) -> new WardHistoryLineResponse(
                        resultSet.getString("v_item_name"),
                        resultSet.getString("v_unit_name"),
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create")),
                        nvlDouble(resultSet.getObject("n_amount_after_disc"), 0)),
                noteId));
        lines.addAll(jdbcTemplate.query(
                "select n_qty, n_amount_after_disc, d_whn_create, v_misc_name, "
                        + "(select v_unit_name from ms_unit where n_unit_id = "
                        + "(select n_unit_id from tb_examination where n_exam_id = ?)) as unit_name "
                        + "from tb_misc_trx where n_note_id = ?",
                (resultSet, rowNum) -> new WardHistoryLineResponse(
                        resultSet.getString("v_misc_name"),
                        resultSet.getString("unit_name"),
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create")),
                        nvlDouble(resultSet.getObject("n_amount_after_disc"), 0)),
                noteId, noteId));
        return lines;
    }

    // ------------------------------------------------------------------ helpers

    private UnitRow findUnit(Integer unitId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_unit_id, v_unit_code, v_unit_name, n_whouse_id from ms_unit "
                            + "where n_unit_id = ?",
                    (resultSet, rowNum) -> new UnitRow(
                            resultSet.getInt("n_unit_id"),
                            resultSet.getString("v_unit_code"),
                            resultSet.getString("v_unit_name"),
                            getNullableInteger(resultSet, "n_whouse_id")),
                    unitId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Unit tidak ditemukan.");
        }
    }

    private double calculateTotal(List<WardLineRequest> lines) {
        double total = 0;
        for (WardLineRequest line : lines) {
            double qty = line.getQty() == null ? 1 : line.getQty();
            double price = line.getPrice() == null ? 0 : line.getPrice();
            double amountBefore = qty * price;
            total += amountBefore - calculateDiscount(amountBefore, line.getDiscType(),
                    line.getDiscAmount());
        }
        return total;
    }

    private double calculateDiscount(double amount, String discType, Double discValue) {
        if (discValue == null || discValue <= 0) {
            return 0;
        }
        if (DISC_PERCENT.equals(discType)) {
            return amount * discValue / 100.0;
        }
        return discValue;
    }

    private String normalizeDiscountType(String discType) {
        return DISC_PERCENT.equals(discType) ? DISC_PERCENT : DISC_RP;
    }

    private String generateRanapNoteNumber(Integer sequence, Timestamp date, String unitCode) {
        String tgl = date.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyMM"));
        return "I-" + unitCode + "-" + tgl + "-" + String.format("%06d", sequence);
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private String getNoteStatusLabel(int status) {
        if (status == NOTE_CANCELED) {
            return "BATAL";
        }
        if (status == NOTE_ACTIVE) {
            return "BARU";
        }
        if (status == NOTE_VALIDATED) {
            return "SUDAH DIVALIDASI";
        }
        return "TIDAK AKTIF";
    }

    private String calculateAgeString(String birthDateIso) {
        if (!hasText(birthDateIso)) {
            return "";
        }
        try {
            LocalDate dob = LocalDate.parse(birthDateIso);
            Period period = Period.between(dob, LocalDate.now());
            return period.getYears() + " thn " + period.getMonths() + " bln " + period.getDays() + " hr";
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeMrCode(String mrCode) {
        return mrCode == null ? "" : mrCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String like(String value) {
        return "%" + (value != null ? value : "") + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String toIsoDate(java.sql.Date date) {
        return date == null ? "" : date.toLocalDate().toString();
    }

    private String toIsoDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().toString();
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double nvlDouble(Object value, double fallback) {
        Double d = toDouble(value);
        return d == null ? fallback : d;
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    // ------------------------------------------------------------------ rows

    private static final class UnitRow {
        private final int unitId;
        private final String code;
        private final String name;
        private final Integer warehouseId;

        private UnitRow(int unitId, String code, String name, Integer warehouseId) {
            this.unitId = unitId;
            this.code = code;
            this.name = name;
            this.warehouseId = warehouseId;
        }
    }

    private static final class RanapRow {
        private final int regId;
        private final String regNo;
        private final int mrId;
        private final String mrCode;
        private final int patientId;
        private final String patientName;
        private final String gender;
        private final String birthDate;
        private final String address;
        private final Integer patientTypeId;
        private final String patientTypeName;
        private final Integer doctorId;
        private final String doctorCode;
        private final String doctorName;

        private RanapRow(int regId, String regNo, int mrId, String mrCode, int patientId,
                String patientName, String gender, String birthDate, String address,
                Integer patientTypeId, String patientTypeName, Integer doctorId,
                String doctorCode, String doctorName) {
            this.regId = regId;
            this.regNo = regNo;
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientId = patientId;
            this.patientName = patientName;
            this.gender = gender;
            this.birthDate = birthDate;
            this.address = address;
            this.patientTypeId = patientTypeId;
            this.patientTypeName = patientTypeName;
            this.doctorId = doctorId;
            this.doctorCode = doctorCode;
            this.doctorName = doctorName;
        }
    }

    private static final class BedRow {
        private final String treatmentClass;
        private final String hall;
        private final String bed;

        private BedRow(String treatmentClass, String hall, String bed) {
            this.treatmentClass = treatmentClass;
            this.hall = hall;
            this.bed = bed;
        }
    }

    private static final class NoteHeader {
        private final int noteId;
        private final String noteNo;
        private final int status;
        private final Double total;
        private final Integer unitId;
        private final String unitName;
        private final Integer patientId;
        private final Integer regId;
        private final String regNo;
        private final Integer doctorId;
        private final String doctorCode;
        private final String doctorName;

        private NoteHeader(int noteId, String noteNo, int status, Double total, Integer unitId,
                String unitName, Integer patientId, Integer regId, String regNo, Integer doctorId,
                String doctorCode, String doctorName) {
            this.noteId = noteId;
            this.noteNo = noteNo;
            this.status = status;
            this.total = total;
            this.unitId = unitId;
            this.unitName = unitName;
            this.patientId = patientId;
            this.regId = regId;
            this.regNo = regNo;
            this.doctorId = doctorId;
            this.doctorCode = doctorCode;
            this.doctorName = doctorName;
        }
    }

    private static final class InventoryRow {
        private final int itemId;
        private final int batchId;
        private final double qty;

        private InventoryRow(int itemId, int batchId, double qty) {
            this.itemId = itemId;
            this.batchId = batchId;
            this.qty = qty;
        }
    }

    private static final class TreatmentJournalRow {
        private final Integer coaId;
        private final String code;
        private final double qty;
        private final double discAmount;
        private final double amountAfterDisc;

        private TreatmentJournalRow(Integer coaId, String code, double qty, double discAmount,
                double amountAfterDisc) {
            this.coaId = coaId;
            this.code = code;
            this.qty = qty;
            this.discAmount = discAmount;
            this.amountAfterDisc = amountAfterDisc;
        }
    }

    private static final class ItemJournalRow {
        private final int itemId;
        private final String code;
        private final double qty;
        private final double discAmount;
        private final double amountAfterDisc;
        private final Integer sellCoaId;
        private final Integer cogsCoaId;
        private final Double cogsPrice;

        private ItemJournalRow(int itemId, String code, double qty, double discAmount,
                double amountAfterDisc, Integer sellCoaId, Integer cogsCoaId, Double cogsPrice) {
            this.itemId = itemId;
            this.code = code;
            this.qty = qty;
            this.discAmount = discAmount;
            this.amountAfterDisc = amountAfterDisc;
            this.sellCoaId = sellCoaId;
            this.cogsCoaId = cogsCoaId;
            this.cogsPrice = cogsPrice;
        }
    }

    private static final class MiscJournalRow {
        private final String name;
        private final double qty;
        private final double amountAfterDisc;

        private MiscJournalRow(String name, double qty, double amountAfterDisc) {
            this.name = name;
            this.qty = qty;
            this.amountAfterDisc = amountAfterDisc;
        }
    }
}
