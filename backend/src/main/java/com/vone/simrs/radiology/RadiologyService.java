package com.vone.simrs.radiology;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.physiotherapy.PhysiotherapyEscortResponse;
import com.vone.simrs.physiotherapy.PhysiotherapyPatientTypeResponse;
import com.vone.simrs.ward.WardActionResultResponse;
import com.vone.simrs.ward.WardDoctorOptionResponse;
import com.vone.simrs.ward.WardHistoryLineResponse;
import com.vone.simrs.ward.WardHistoryNoteResponse;
import com.vone.simrs.ward.WardHistoryResponse;
import com.vone.simrs.ward.WardItemOptionResponse;
import com.vone.simrs.ward.WardLineRequest;
import com.vone.simrs.ward.WardNoteDetailResponse;
import com.vone.simrs.ward.WardNoteLineResponse;
import com.vone.simrs.ward.WardNoteSaveRequest;
import com.vone.simrs.ward.WardNoteSummaryResponse;
import com.vone.simrs.ward.WardPatientDetailResponse;
import com.vone.simrs.ward.WardPatientOptionResponse;
import com.vone.simrs.ward.WardTreatmentOptionResponse;
import com.vone.simrs.ward.WardUnitResponse;
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
 * Service untuk screen SC0051 (TRANSAKSI RADIOLOGI / radiology.zul).
 *
 * <p>
 * Migrasi dari legacy {@code RadiologyController} +
 * {@code RadiologyManagerImpl.getRegistration()} + {@code PolyclinicDAO.saveTransaction()}
 * + {@code NoteDAO.save()} (journal). Struktur sama dengan transaksi fisioterapi:
 * pasien terdaftar (rajal/ranap), nomor nota menyesuaikan tipe registrasi.
 */
@Service
public class RadiologyService {

    private static final int DOCTOR_GROUP = 4;
    private static final int RADIOGRAPHER_GROUP = 10;
    private static final int REG_ACTIVE = 1;
    private static final short NOTE_ACTIVE = 1;
    private static final short NOTE_VALIDATED = 2;
    private static final short NOTE_CANCELED = 0;
    private static final short BELUM_LUNAS = 0;
    private static final String DISC_RP = "RP";
    private static final String DISC_PERCENT = "%";
    private static final String LINE_TREATMENT = "TREATMENT";
    private static final String LINE_ITEM = "ITEM";
    private static final String LINE_MISC = "MISC";
    private static final String MISC_CODE = "MISC-001";
    private static final String DEFAULT_TARIFF_CLASS = "KELAS III";
    private static final String NON_PAKET = "NON-PAKET";
    private static final DateTimeFormatter NOTE_DATE = DateTimeFormatter.ofPattern("yyMM");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public RadiologyService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ------------------------------------------------------------------ masters

    public RadiologyMastersResponse getMasters(String username) {
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
        List<PhysiotherapyPatientTypeResponse> patientTypes = jdbcTemplate.query(
                "select n_patient_type_id, v_tpatient, v_tpatient_desc from ms_patient_type "
                        + "order by v_tpatient",
                (resultSet, rowNum) -> new PhysiotherapyPatientTypeResponse(
                        resultSet.getInt("n_patient_type_id"),
                        resultSet.getString("v_tpatient"),
                        resultSet.getString("v_tpatient_desc")));
        List<PhysiotherapyEscortResponse> escorts = jdbcTemplate.query(
                "select n_escort_primary_id, v_escort_code, v_escort_type from ms_patient_escort "
                        + "order by v_escort_type",
                (resultSet, rowNum) -> new PhysiotherapyEscortResponse(
                        resultSet.getInt("n_escort_primary_id"),
                        resultSet.getString("v_escort_code"),
                        resultSet.getString("v_escort_type")));
        return new RadiologyMastersResponse(units, patientTypes, escorts);
    }

    // ------------------------------------------------------------------ pasien

    /** Cari pasien terdaftar (registrasi aktif). Migrasi dari searchPatientRegistered. */
    public List<WardPatientOptionResponse> searchRegisteredPatients(String mrCode,
            String patientName, String address, String birthDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, ")
                .append("pat.v_patient_main_addr, pat.n_patient_type_id ")
                .append("from tb_medical_record mr ")
                .append("join ms_patient pat on pat.n_patient_id = mr.n_patient_id ")
                .append("join tb_registration reg on reg.n_mr_id = mr.n_mr_id ")
                .append("where reg.reg_status = ? and mr.v_mr_code like ? ")
                .append("and pat.v_patient_name like ? and pat.v_patient_main_addr like ? ");
        List<Object> params = new ArrayList<>();
        params.add(REG_ACTIVE);
        params.add(like(normalizeOptionalUpper(mrCode)));
        params.add(like(normalizeOptionalUpper(patientName)));
        params.add(like(normalizeOptionalUpper(address)));
        if (hasText(birthDate)) {
            sql.append("and pat.d_patient_dob = ? ");
            params.add(LocalDate.parse(birthDate));
        }
        sql.append("limit 100");
        return jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> {
                    Integer typeId = getNullableInteger(resultSet, "n_patient_type_id");
                    return new WardPatientOptionResponse(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("v_patient_name"),
                            typeId != null && typeId == 8 ? "BPJS" : "NON BPJS",
                            resultSet.getString("v_patient_main_addr"));
                });
    }

    /** Detail pasien + registrasi terakhir + bed (jika rawat inap). */
    public WardPatientDetailResponse getPatientDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        PatientRow patient = findPatientByMrCode(normalizeMrCode(mrCode));
        RegistrationRow reg = findLastRegistration(patient.mrId);
        if (reg == null) {
            throw new IllegalArgumentException("REGISTRASI TIDAK AKTIF!");
        }
        BedRow bed = reg.regNo != null && reg.regNo.startsWith("I")
                ? findBedByRegId(reg.regId) : null;
        DoctorRow doctor = reg.doctorId == null ? null : findDoctor(reg.doctorId);
        return new WardPatientDetailResponse(
                patient.mrId, patient.mrCode, reg.regId, reg.regNo, patient.patientId,
                patient.patientName, patient.gender, patient.birthDate,
                calculateAgeString(patient.birthDate), patient.address,
                patient.patientTypeId, patient.patientTypeName,
                doctor == null ? null : doctor.staffId,
                doctor == null ? null : (doctor.code + "-" + doctor.name),
                bed == null ? "" : bed.treatmentClass,
                bed == null ? "" : bed.hall,
                bed == null ? "" : bed.bed);
    }

    private PatientRow findPatientByMrCode(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select mr.n_mr_id, mr.v_mr_code, pat.n_patient_id, pat.v_patient_name, "
                            + "pat.v_patient_gender, pat.d_patient_dob, pat.v_patient_main_addr, "
                            + "pat.n_patient_type_id, pt.v_tpatient_desc "
                            + "from tb_medical_record mr "
                            + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                            + "left join ms_patient_type pt on pt.n_patient_type_id = pat.n_patient_type_id "
                            + "where upper(mr.v_mr_code) = ?",
                    (resultSet, rowNum) -> new PatientRow(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_gender"),
                            toIsoDate(resultSet.getDate("d_patient_dob")),
                            resultSet.getString("v_patient_main_addr"),
                            getNullableInteger(resultSet, "n_patient_type_id"),
                            resultSet.getString("v_tpatient_desc")),
                    mrCode);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("MR TIDAK DITEMUKAN!");
        }
    }

    private RegistrationRow findLastRegistration(Integer mrId) {
        List<RegistrationRow> rows = jdbcTemplate.query(
                "select n_reg_id, v_reg_secondary_id, n_staff_id from tb_registration "
                        + "where n_mr_id = ? and reg_status = ? "
                        + "order by d_registration_date desc limit 1",
                (resultSet, rowNum) -> new RegistrationRow(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id"),
                        getNullableInteger(resultSet, "n_staff_id")),
                mrId, REG_ACTIVE);
        return rows.isEmpty() ? null : rows.get(0);
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

    private DoctorRow findDoctor(Integer staffId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_staff_id, v_staff_code, v_staff_name from ms_staff where n_staff_id = ?",
                    (resultSet, rowNum) -> new DoctorRow(
                            resultSet.getInt("n_staff_id"),
                            resultSet.getString("v_staff_code"),
                            resultSet.getString("v_staff_name")),
                    staffId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
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

    /**
     * Daftar radiografer untuk pilihan pada form TAMBAH TINDAKAN. Migrasi dari
     * legacy {@code RadiologyDAO.getMedician("RADIOGRAFER")} — ms_doctor dengan
     * grup RADIOGRAFER (n_msgroup_id = 10).
     */
    public List<WardDoctorOptionResponse> searchRadiographers() {
        return jdbcTemplate.query(
                "select st.n_staff_id, st.v_staff_code, st.v_staff_name "
                        + "from ms_doctor dr "
                        + "join ms_staff st on st.n_staff_id = dr.n_staff_id "
                        + "where dr.n_msgroup_id = ? and st.d_staff_fired_date is null "
                        + "order by st.v_staff_name",
                (resultSet, rowNum) -> new WardDoctorOptionResponse(
                        resultSet.getInt("n_staff_id"),
                        resultSet.getString("v_staff_code"),
                        resultSet.getString("v_staff_name")),
                RADIOGRAPHER_GROUP);
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

    public WardNoteDetailResponse getNoteDetail(Integer noteId) {
        NoteHeader header = findNoteHeader(noteId);
        PatientRow patient = header.patientId == null ? null : findPatientById(header.patientId);
        RegistrationRow reg = header.regId == null ? null : findRegistration(header.regId);
        BedRow bed = reg != null && reg.regNo != null && reg.regNo.startsWith("I")
                ? findBedByRegId(reg.regId) : null;
        DoctorRow doctor = (reg != null && reg.doctorId != null) ? findDoctor(reg.doctorId) : null;
        List<WardNoteLineResponse> lines = getNoteLines(noteId);

        int status = header.status;
        boolean validated = status == NOTE_VALIDATED;
        boolean canceled = status == NOTE_CANCELED;
        return new WardNoteDetailResponse(
                header.noteId, header.noteNo, status, getNoteStatusLabel(status),
                header.total, header.unitId, header.unitName, header.patientId,
                patient == null ? "" : patient.mrCode,
                patient == null ? "" : patient.patientName,
                patient == null ? "" : patient.gender,
                patient == null ? "" : patient.birthDate,
                patient == null ? "" : calculateAgeString(patient.birthDate),
                patient == null ? "" : patient.address,
                patient == null ? null : patient.patientTypeId,
                patient == null ? "" : patient.patientTypeName,
                header.regId,
                reg == null ? "" : reg.regNo,
                doctor == null ? null : doctor.staffId,
                doctor == null ? null : (doctor.code + "-" + doctor.name),
                bed == null ? "" : bed.treatmentClass,
                bed == null ? "" : bed.hall,
                bed == null ? "" : bed.bed,
                !validated && !canceled, !validated && !canceled, !canceled, lines);
    }

    private PatientRow findPatientById(Integer patientId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select mr.n_mr_id, mr.v_mr_code, pat.n_patient_id, pat.v_patient_name, "
                            + "pat.v_patient_gender, pat.d_patient_dob, pat.v_patient_main_addr, "
                            + "pat.n_patient_type_id, pt.v_tpatient_desc "
                            + "from ms_patient pat "
                            + "left join tb_medical_record mr on mr.n_patient_id = pat.n_patient_id "
                            + "left join ms_patient_type pt on pt.n_patient_type_id = pat.n_patient_type_id "
                            + "where pat.n_patient_id = ?",
                    (resultSet, rowNum) -> new PatientRow(
                            getNullableInteger(resultSet, "n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getInt("n_patient_id"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_gender"),
                            toIsoDate(resultSet.getDate("d_patient_dob")),
                            resultSet.getString("v_patient_main_addr"),
                            getNullableInteger(resultSet, "n_patient_type_id"),
                            resultSet.getString("v_tpatient_desc")),
                    patientId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private RegistrationRow findRegistration(Integer regId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_reg_id, v_reg_secondary_id, n_staff_id from tb_registration "
                            + "where n_reg_id = ?",
                    (resultSet, rowNum) -> new RegistrationRow(
                            resultSet.getInt("n_reg_id"),
                            resultSet.getString("v_reg_secondary_id"),
                            getNullableInteger(resultSet, "n_staff_id")),
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
                            + "note.n_reg_id, note.n_escort_id, unit.v_unit_name "
                            + "from tb_examination note "
                            + "left join ms_unit unit on unit.n_unit_id = note.n_unit_id "
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
                            getNullableInteger(resultSet, "n_escort_id")),
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
                        + "trx.n_doctor_id, trx.n_anesthacy_id, tfee.n_doctor_fee, "
                        + "st.v_staff_name, rg.v_staff_name as radiografer_name "
                        + "from tb_treatment_trx trx "
                        + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                        + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                        + "left join ms_staff st on st.n_staff_id = trx.n_doctor_id "
                        + "left join ms_staff rg on rg.n_staff_id = trx.n_anesthacy_id "
                        + "where trx.n_note_id = ? order by trx.n_treatment_id",
                (resultSet, rowNum) -> {
                    double qty = nvlDouble(resultSet.getObject("n_qty"), 1);
                    double amountTrx = nvlDouble(resultSet.getObject("n_amount_trx"), 0);
                    String name = resultSet.getString("v_treatment_name");
                    String doctorName = resultSet.getString("v_staff_name");
                    String radiograferName = resultSet.getString("radiografer_name");
                    // migrasi legacy: keterangan = nama tindakan - dokter pemeriksa - radiografer
                    if (resultSet.getObject("n_doctor_id") != null && doctorName != null) {
                        name = name + "-" + doctorName;
                    }
                    if (resultSet.getObject("n_anesthacy_id") != null && radiograferName != null) {
                        name = name + "-" + radiograferName;
                    }
                    return new WardNoteLineResponse(
                            LINE_TREATMENT,
                            resultSet.getInt("n_treatment_fee_id"),
                            resultSet.getString("v_treatment_code"), name, qty, "-",
                            qty == 0 ? 0 : amountTrx / qty,
                            resultSet.getString("v_disc_type"),
                            toDouble(resultSet.getObject("n_disc_amount")),
                            toDouble(resultSet.getObject("n_amount_after_disc")),
                            getNullableInteger(resultSet, "n_doctor_id"),
                            getNullableInteger(resultSet, "n_anesthacy_id"));
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

    @Transactional
    public WardActionResultResponse saveNote(WardNoteSaveRequest request, String username) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("TIDAK ADA TRANSAKSI YANG AKAN DISIMPAN!");
        }
        PatientRow patient = findPatientByMrCode(normalizeMrCode(request.getMrCode()));
        RegistrationRow reg = findLastRegistration(patient.mrId);
        if (reg == null) {
            throw new IllegalArgumentException("REGISTRASI TIDAK AKTIF!");
        }
        UnitRow unit = findUnit(request.getUnitId());
        Timestamp now = new Timestamp(System.currentTimeMillis());

        double total = calculateTotal(request.getLines());
        Integer noteId = getNextSequence("tb_examination_n_exam_id_seq");
        String noteNo = generateNoteNumber(noteId, now, unit.code, reg.regNo);

        jdbcTemplate.update(
                "insert into tb_examination (n_exam_id, v_note_no, n_exam_status, n_payment_status, "
                        + "n_unit_id, n_patient_id, n_reg_id, n_escort_id, n_total_amount, "
                        + "d_whn_create, v_who_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                noteId, noteNo, (int) NOTE_ACTIVE, (int) BELUM_LUNAS,
                unit.unitId, patient.patientId, reg.regId, request.getEscortId(), total,
                now, username);

        saveNoteLines(noteId, request.getLines(), unit, username, now);
        return new WardActionResultResponse(true, "Nota berhasil disimpan.", noteId, noteNo);
    }

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
                "update tb_examination set n_total_amount = ?, n_escort_id = ?, "
                        + "d_whn_change = ?, v_who_change = ? where n_exam_id = ?",
                total, request.getEscortId(), now, username, noteId);
        saveNoteLines(noteId, request.getLines(), unit, username, now);
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
                        + "n_doctor_id, n_anesthacy_id, d_whn_create, v_who_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                noteId, line.getReferenceId(), (short) qty, amountBefore, discAmount,
                normalizeDiscountType(line.getDiscType()), amountBefore - discAmount,
                line.getDoctorId(), line.getRadiograferId(), now, username);
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
        boolean ranap = isRanap(header.regId);
        Integer coaArId = findCoaIdByGimKey(ranap ? "COA_INPATIENT_AR" : "COA_OUTPATIENT_AR");
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

    private boolean isRanap(Integer regId) {
        if (regId == null) {
            return false;
        }
        RegistrationRow reg = findRegistration(regId);
        return reg != null && reg.regNo != null && reg.regNo.startsWith("I");
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

    // ------------------------------------------------------------------ history

    public WardHistoryResponse getHistory(String mrCode, String mode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        PatientRow patient = findPatientByMrCode(normalizeMrCode(mrCode));
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
                patient.patientId, (int) NOTE_CANCELED);

        double grandTotal = 0;
        for (WardHistoryNoteResponse note : notes) {
            grandTotal += note.getTotal();
        }
        return new WardHistoryResponse(patient.mrCode, patient.patientName,
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

    private String generateNoteNumber(Integer sequence, Timestamp date, String unitCode,
            String regNo) {
        boolean ranap = regNo != null && regNo.startsWith("I");
        String prefix = ranap ? "I-" : "J-";
        String tgl = date.toLocalDateTime().format(NOTE_DATE);
        return prefix + unitCode + "-" + tgl + "-" + String.format("%06d", sequence);
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

    private static final class PatientRow {
        private final Integer mrId;
        private final String mrCode;
        private final int patientId;
        private final String patientName;
        private final String gender;
        private final String birthDate;
        private final String address;
        private final Integer patientTypeId;
        private final String patientTypeName;

        private PatientRow(Integer mrId, String mrCode, int patientId, String patientName,
                String gender, String birthDate, String address, Integer patientTypeId,
                String patientTypeName) {
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientId = patientId;
            this.patientName = patientName;
            this.gender = gender;
            this.birthDate = birthDate;
            this.address = address;
            this.patientTypeId = patientTypeId;
            this.patientTypeName = patientTypeName;
        }
    }

    private static final class RegistrationRow {
        private final int regId;
        private final String regNo;
        private final Integer doctorId;

        private RegistrationRow(int regId, String regNo, Integer doctorId) {
            this.regId = regId;
            this.regNo = regNo;
            this.doctorId = doctorId;
        }
    }

    private static final class DoctorRow {
        private final int staffId;
        private final String code;
        private final String name;

        private DoctorRow(int staffId, String code, String name) {
            this.staffId = staffId;
            this.code = code;
            this.name = name;
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
        private final Integer escortId;

        private NoteHeader(int noteId, String noteNo, int status, Double total, Integer unitId,
                String unitName, Integer patientId, Integer regId, Integer escortId) {
            this.noteId = noteId;
            this.noteNo = noteNo;
            this.status = status;
            this.total = total;
            this.unitId = unitId;
            this.unitName = unitName;
            this.patientId = patientId;
            this.regId = regId;
            this.escortId = escortId;
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
