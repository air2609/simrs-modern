package com.vone.simrs.polyclinic;

import com.vone.simrs.accounting.JournalService;
import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PolyclinicService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String SCREEN_POLIKLINIK = "SC0091";
    private static final String NON_PACKET = "NON-PAKET";
    private static final String DEFAULT_TARIFF_CLASS = "KELAS II";
    private static final String DISCOUNT_RP = "RP";
    private static final String DISCOUNT_PERCENT = "%";
    private static final int REG_ACTIVE = 1;
    private static final int NOTE_ACTIVE = 1;
    private static final int NOTE_VALIDATED = 2;
    private static final int NOTE_CANCELED = 0;
    private static final int NOTE_VALIDATED_CANCELED = 4;
    private static final short PAYMENT_UNPAID = 0;
    private static final SimpleDateFormat NOTE_DATE_FORMAT = new SimpleDateFormat("yyMM");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final JournalService journalService;

    public PolyclinicService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService, JournalService journalService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.journalService = journalService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    public PolyclinicMastersResponse getMasters(String username) {
        return new PolyclinicMastersResponse(
            getUnits(username),
            getPatientTypes(),
            getEscorts()
        );
    }

    public List<PolyclinicRegisteredPatientResponse> searchRegisteredPatients(
        Integer unitId,
        String mrCode,
        String patientName,
        String address
    ) {
        if (!hasText(mrCode) && !hasText(patientName) && !hasText(address)) {
            throw new IllegalArgumentException("Salah satu field pencarian pasien harus diisi.");
        }

        return jdbcTemplate.query(
            "select distinct mr.n_mr_id, mr.v_mr_code, p.v_patient_name, p.v_patient_main_addr "
                + "from tb_medical_record mr, ms_patient p, tb_registration reg "
                + "where mr.v_mr_code ilike ? "
                + "and mr.n_patient_id = p.n_patient_id "
                + "and p.v_patient_name ilike ? "
                + "and p.v_patient_main_addr ilike ? "
                + "and mr.n_mr_id = reg.n_mr_id "
                + "and reg.reg_status = ? "
                + "and reg.n_unit_id = ? "
                + "limit 100",
            (resultSet, rowNum) -> new PolyclinicRegisteredPatientResponse(
                resultSet.getInt("n_mr_id"),
                resultSet.getString("v_mr_code"),
                resultSet.getString("v_patient_name"),
                resultSet.getString("v_patient_main_addr")
            ),
            likeMrCode(mrCode),
            likeText(patientName),
            likeText(address),
            REG_ACTIVE,
            unitId
        );
    }

    public PolyclinicPatientDetailResponse getRegisteredPatientDetail(Integer unitId, String mrCode) {
        List<PolyclinicPatientDetailResponse> results = jdbcTemplate.query(
            "select mr.n_mr_id, mr.v_mr_code, p.n_patient_id, p.n_patient_type_id, p.v_patient_name, "
                + "p.v_patient_gender, p.d_patient_dob, p.v_patient_main_addr, "
                + "reg.n_reg_id, reg.v_reg_secondary_id, reg.d_registration_date, "
                + "doc.n_staff_id as n_doctor_id, doc.v_staff_code as v_doctor_code, doc.v_staff_name as v_doctor_name "
                + "from tb_medical_record mr "
                + "join ms_patient p on p.n_patient_id = mr.n_patient_id "
                + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                + "left join ms_staff doc on doc.n_staff_id = reg.n_staff_id "
                + "where reg.reg_status = ? "
                + "and reg.n_unit_id = ? "
                + "and mr.v_mr_code = ? "
                + "order by reg.d_registration_date desc "
                + "limit 1",
            (resultSet, rowNum) -> new PolyclinicPatientDetailResponse(
                resultSet.getInt("n_patient_id"),
                resultSet.getInt("n_mr_id"),
                resultSet.getString("v_mr_code"),
                resultSet.getInt("n_reg_id"),
                resultSet.getString("v_reg_secondary_id"),
                getNullableInteger(resultSet, "n_patient_type_id"),
                resultSet.getString("v_patient_name"),
                resultSet.getString("v_patient_gender"),
                toIsoDate(resultSet.getDate("d_patient_dob")),
                resultSet.getString("v_patient_main_addr"),
                getNullableInteger(resultSet, "n_doctor_id"),
                resultSet.getString("v_doctor_code"),
                resultSet.getString("v_doctor_name"),
                resultSet.getTimestamp("d_registration_date") == null
                    ? null
                    : resultSet.getTimestamp("d_registration_date").toInstant().toString()
            ),
            REG_ACTIVE,
            unitId,
            normalizeMrCode(mrCode)
        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Data pasien registrasi tidak ditemukan pada unit yang dipilih.");
        }
        return results.get(0);
    }

    public List<PolyclinicDoctorResponse> searchDoctors(Integer unitId, String code, String name) {
        return jdbcTemplate.query(
            "select distinct staff.n_staff_id, staff.v_staff_code, staff.v_staff_name "
                + "from ms_doctor dr "
                + "join ms_staff staff on staff.n_staff_id = dr.n_staff_id "
                + "join ms_staff_in_unit siu on siu.n_staff_id = staff.n_staff_id "
                + "where siu.n_unit_id = ? "
                + "and staff.d_staff_fired_date is null "
                + "and staff.v_staff_code like ? "
                + "and upper(staff.v_staff_name) like ? "
                + "order by staff.v_staff_name "
                + "limit 100",
            (resultSet, rowNum) -> new PolyclinicDoctorResponse(
                resultSet.getInt("n_staff_id"),
                resultSet.getString("v_staff_code"),
                resultSet.getString("v_staff_name")
            ),
            unitId,
            likeRaw(code),
            likeUpper(name)
        );
    }

    public List<PolyclinicTreatmentOptionResponse> searchTreatments(
        Integer unitId,
        String code,
        String name,
        String tariffClass
    ) {
        if (!hasText(code) && !hasText(name)) {
            throw new IllegalArgumentException("Salah satu field pencarian tindakan harus diisi.");
        }

        return jdbcTemplate.query(
            "select tfee.n_treatment_fee_id, treat.n_treatment_id, treat.v_treatment_code, treat.v_treatment_name, "
                + "coalesce(tfee.n_trtfee_fee, 0) as n_trtfee_fee, coalesce(tfee.n_doctor_fee, 0) as n_doctor_fee "
                + "from ms_treatment_fee tfee, ms_treatment treat, ms_treatment_class tclass, ms_treatment_group grup "
                + "where tfee.n_treatment_id = treat.n_treatment_id "
                + "and tfee.n_trtfee_fee > 0 "
                + "and treat.v_treatment_code like ? "
                + "and upper(treat.v_treatment_name) like ? "
                + "and tfee.n_tclass_id = tclass.n_tclass_id "
                + "and tclass.v_tclass_desc = ? "
                + "and treat.n_tgroup_id = grup.n_tgroup_id "
                + "and grup.v_tgroup_name = ? "
                + "limit 100",
            (resultSet, rowNum) -> new PolyclinicTreatmentOptionResponse(
                resultSet.getInt("n_treatment_fee_id"),
                resultSet.getInt("n_treatment_id"),
                resultSet.getString("v_treatment_code"),
                resultSet.getString("v_treatment_name"),
                resultSet.getDouble("n_trtfee_fee"),
                resultSet.getDouble("n_doctor_fee")
            ),
            likeRaw(code),
            likeUpper(name),
            normalizeTariffClass(tariffClass),
            NON_PACKET
        );
    }

    public List<PolyclinicItemOptionResponse> searchItems(
        Integer unitId,
        String code,
        String name,
        String tariffClass
    ) {
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        if (warehouseId == null) {
            throw new IllegalStateException("Unit poliklinik belum terhubung dengan gudang inventory.");
        }
        if (!hasText(code) && !hasText(name)) {
            throw new IllegalArgumentException("Salah satu field pencarian item harus diisi.");
        }

        return jdbcTemplate.query(
            "select inv.n_item_id as id, item.v_item_code as code, item.v_item_name as name, "
                + "item.n_r as jasa_r, sell.n_selling_price as harga, sat.v_mitem_end_quantify as satuan, "
                + "sum(inv.n_item_inv_qty) as jumlah, item.n_type as tipe "
                + "from tb_item_inventory inv, ms_item item, ms_item_selling_price sell, "
                + "ms_item_measurement sat, ms_treatment_class tclass "
                + "where inv.n_whouse_id = ? "
                + "and inv.n_item_id = item.n_item_id "
                + "and inv.n_item_inv_qty > 0 "
                + "and item.v_item_code like ? "
                + "and upper(item.v_item_name) like ? "
                + "and item.n_item_id = sell.n_item_id "
                + "and item.n_mitem_id = sat.n_mitem_id "
                + "and sell.n_tclass_id = tclass.n_tclass_id "
                + "and tclass.v_tclass_desc = ? "
                + "group by id, code, name, jasa_r, harga, satuan, tipe "
                + "order by name "
                + "limit 100",
            (resultSet, rowNum) -> new PolyclinicItemOptionResponse(
                resultSet.getInt("id"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("satuan"),
                resultSet.getDouble("harga"),
                resultSet.getDouble("jumlah"),
                getNullableShort(resultSet, "jasa_r"),
                getNullableShort(resultSet, "tipe")
            ),
            warehouseId,
            likeRaw(code),
            likeUpper(name),
            normalizeTariffClass(tariffClass)
        );
    }

    public List<PolyclinicNoteSummaryResponse> searchNotes(Integer unitId, String noteNumber, String patientName) {
        return jdbcTemplate.query(
            "select note.n_exam_id, note.v_note_no, note.n_exam_status, p.v_patient_name, note.d_whn_create "
                + "from tb_examination note "
                + "join ms_patient p on p.n_patient_id = note.n_patient_id "
                + "where note.n_unit_id = ? "
                + "and note.v_note_no like ? "
                + "and upper(p.v_patient_name) like ? "
                + "and note.n_exam_status in (?, ?) "
                + "order by note.d_whn_create desc "
                + "limit 100",
            (resultSet, rowNum) -> new PolyclinicNoteSummaryResponse(
                resultSet.getInt("n_exam_id"),
                resultSet.getString("v_note_no"),
                resultSet.getString("v_patient_name"),
                resultSet.getInt("n_exam_status"),
                toStatusLabel(resultSet.getInt("n_exam_status")),
                resultSet.getTimestamp("d_whn_create") == null
                    ? null
                    : resultSet.getTimestamp("d_whn_create").toInstant().toString()
            ),
            unitId,
            likeRaw(noteNumber),
            likeUpper(patientName),
            NOTE_ACTIVE,
            NOTE_VALIDATED
        );
    }

    public PolyclinicNoteDetailResponse getNoteDetail(Integer noteId) {
        PolyclinicNoteHeader header = findNoteHeader(noteId);
        List<PolyclinicNoteLineResponse> lines = new ArrayList<PolyclinicNoteLineResponse>();
        lines.addAll(getTreatmentLines(noteId));
        lines.addAll(getItemLines(noteId));
        lines.addAll(getMiscLines(noteId));
        lines.addAll(getBundleLines(noteId));

        return new PolyclinicNoteDetailResponse(
            header.noteId,
            header.noteNumber,
            header.statusCode,
            toStatusLabel(header.statusCode),
            header.totalAmount,
            header.unitId,
            header.unitCode,
            header.unitName,
            header.patientId,
            header.patientTypeId,
            header.patientName,
            header.gender,
            header.birthDate,
            header.address,
            header.medicalRecordCode,
            header.registrationId,
            header.registrationCode,
            header.doctorId,
            header.doctorCode,
            header.doctorName,
            header.escortId,
            header.cancelationNote,
            header.statusCode == NOTE_ACTIVE && !hasBundleLines(noteId),
            header.statusCode == NOTE_ACTIVE,
            header.statusCode == NOTE_ACTIVE || header.statusCode == NOTE_VALIDATED,
            lines
        );
    }

    @Transactional
    public PolyclinicSaveResultResponse createNote(@Valid PolyclinicSaveRequest request, String username) {
        validateSaveRequest(request);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Integer unitId = request.getUnitId();
        UnitRow unit = findUnit(unitId);
        Integer patientId;
        Integer registrationId = null;
        String registrationCode = null;
        Integer defaultDoctorId = null;
        String medicalRecordCode = null;

        if (Boolean.TRUE.equals(request.getReferencePatient())) {
            patientId = nextSequenceValue("ms_patient_n_patient_id_seq");
            insertReferencePatient(patientId, request, username, now);
        } else {
            RegisteredContext registeredContext = findRegisteredContext(unitId, request.getExistingMrCode());
            patientId = registeredContext.patientId;
            registrationId = registeredContext.registrationId;
            registrationCode = registeredContext.registrationCode;
            defaultDoctorId = registeredContext.doctorId;
            medicalRecordCode = registeredContext.mrCode;
        }

        Integer noteId = nextSequenceValue("tb_examination_n_exam_id_seq");
        Integer noteSequence = nextSequenceValue("nota_rajal_seq");
        String noteNumber = generateNotaNumber(noteSequence, now, unit.unitCode);
        double totalAmount = calculateHeaderTotal(request.getLines());

        jdbcTemplate.update(
            "insert into tb_examination (n_exam_id, n_reg_id, n_escort_id, n_patient_id, n_unit_id, v_note_no, "
                + "n_total_amount, n_payment_status, n_exam_status, v_who_create, d_whn_create) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            noteId,
            registrationId,
            request.getEscortId(),
            patientId,
            unitId,
            noteNumber,
            totalAmount,
            PAYMENT_UNPAID,
            NOTE_ACTIVE,
            normalizeUpper(username),
            now
        );

        persistLines(noteId, unit, request.getLines(), defaultDoctorId, username, now);

        return new PolyclinicSaveResultResponse(noteId, noteNumber, NOTE_ACTIVE, toStatusLabel(NOTE_ACTIVE), medicalRecordCode, registrationCode);
    }

    @Transactional
    public PolyclinicSaveResultResponse updateNote(Integer noteId, @Valid PolyclinicSaveRequest request, String username) {
        validateSaveRequest(request);
        PolyclinicNoteHeader header = findNoteHeader(noteId);

        if (header.statusCode != NOTE_ACTIVE) {
            throw new IllegalStateException("Hanya nota status BARU yang bisa diubah.");
        }
        if (hasBundleLines(noteId)) {
            throw new IllegalStateException("Nota yang memiliki paket legacy belum bisa diubah dari UI modern.");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        UnitRow unit = findUnit(header.unitId);

        if (header.registrationId == null && !Boolean.TRUE.equals(request.getReferencePatient())) {
            throw new IllegalArgumentException("Nota pasien bebas tidak bisa diubah menjadi pasien registrasi.");
        }
        if (header.registrationId != null && Boolean.TRUE.equals(request.getReferencePatient())) {
            throw new IllegalArgumentException("Nota pasien registrasi tidak bisa diubah menjadi pasien bebas.");
        }

        Integer defaultDoctorId = header.doctorId;
        if (header.registrationId == null) {
            updateReferencePatient(header.patientId, request, username, now);
        } else {
            RegisteredContext registeredContext = findRegisteredContext(header.unitId, request.getExistingMrCode());
            if (!header.registrationId.equals(registeredContext.registrationId)) {
                throw new IllegalArgumentException("Nota harus tetap terhubung dengan registrasi pasien yang sama.");
            }
            defaultDoctorId = registeredContext.doctorId;
        }

        restoreInventoryForItems(noteId, unit.unitId);
        deleteNoteLineTables(noteId);

        double totalAmount = calculateHeaderTotal(request.getLines());
        jdbcTemplate.update(
            "update tb_examination set n_escort_id = ?, n_total_amount = ?, v_who_change = ?, d_whn_change = ? where n_exam_id = ?",
            request.getEscortId(),
            totalAmount,
            normalizeUpper(username),
            now,
            noteId
        );

        persistLines(noteId, unit, request.getLines(), defaultDoctorId, username, now);

        return new PolyclinicSaveResultResponse(
            noteId,
            header.noteNumber,
            NOTE_ACTIVE,
            toStatusLabel(NOTE_ACTIVE),
            header.medicalRecordCode,
            header.registrationCode
        );
    }

    @Transactional
    public PolyclinicActionResultResponse validateNote(Integer noteId, String username) {
        PolyclinicNoteHeader header = findNoteHeader(noteId);
        if (header.statusCode != NOTE_ACTIVE) {
            throw new IllegalStateException("Hanya nota status BARU yang bisa divalidasi.");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
            "update tb_examination set n_exam_status = ?, d_validation_date = ?, v_who_change = ?, d_whn_change = ? where n_exam_id = ?",
            NOTE_VALIDATED,
            now,
            normalizeUpper(username),
            now,
            noteId
        );

        // Auto jurnal setelah validasi
        createPolyclinicJournal(noteId, header, now, username);

        return new PolyclinicActionResultResponse(noteId, header.noteNumber, NOTE_VALIDATED, toStatusLabel(NOTE_VALIDATED));
    }

    @Transactional
    public PolyclinicActionResultResponse cancelNote(Integer noteId, @Valid PolyclinicCancelRequest request, String username) {
        PolyclinicNoteHeader header = findNoteHeader(noteId);
        if (header.statusCode != NOTE_ACTIVE && header.statusCode != NOTE_VALIDATED) {
            throw new IllegalStateException("Status nota saat ini tidak bisa dibatalkan.");
        }

        restoreInventoryForCancel(noteId, header.unitId);

        int nextStatus = header.statusCode == NOTE_VALIDATED ? NOTE_VALIDATED_CANCELED : NOTE_CANCELED;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
            "update tb_examination set n_exam_status = ?, v_cancelation_note = ?, v_who_change = ?, d_whn_change = ? where n_exam_id = ?",
            nextStatus,
            normalizeUpper(request.getReason()),
            normalizeUpper(username),
            now,
            noteId
        );

        return new PolyclinicActionResultResponse(noteId, header.noteNumber, nextStatus, toStatusLabel(nextStatus));
    }

    /* AUTO JOURNAL — dipanggil saat validasi nota */
    private void createPolyclinicJournal(Integer noteId, PolyclinicNoteHeader header, Timestamp now, String username) {
        String batchId = journalService.buildJournalBatchId();
        String voucherNo = header.noteNumber;
        Integer coaArId = journalService.findCoaIdByGimKey("COA_OUTPATIENT_AR");
        if (coaArId == null) coaArId = journalService.findCoaIdByGimKey("COA_INPATIENT_AR");
        if (coaArId == null) throw new IllegalStateException("COA Piutang belum dikonfigurasi.");
        Integer coaTreatId = journalService.findCoaIdByGimKey("COA_TREATMENT");
        if (coaTreatId == null) coaTreatId = journalService.findCoaIdByGimKey("COA_MISC_TRX");
        Integer coaSellId = journalService.findCoaIdByGimKey("COA_ITEM_SELL");
        if (coaSellId == null) coaSellId = journalService.findCoaIdByGimKey("COA_MISC_TRX");
        Integer coaMiscId = journalService.findCoaIdByGimKey("COA_MISC_TRX");
        if (coaMiscId == null) throw new IllegalStateException("COA Misc belum dikonfigurasi.");
        Integer coaInvId = whCoaByUnit(header.unitId);
        postLines(batchId, voucherNo, now, username, getTreatJournalLines(noteId), coaArId, coaTreatId);
        postLines(batchId, voucherNo, now, username, getItemJournalLines(noteId, coaInvId), coaArId, coaSellId);
        postLines(batchId, voucherNo, now, username, getMiscJournalLines(noteId), coaArId, coaMiscId);
    }

    private void postLines(String bid, String vno, Timestamp now, String user,
            List<JournalLine> lines, Integer debitCoa, Integer creditCoa) {
        for (JournalLine l : lines) {
            double amt = Math.ceil(l.amt);
            if (amt <= 0) continue;
            journalService.insertJournalEntry(bid, vno, l.desc, amt, 0, now, user, debitCoa);
            journalService.insertJournalEntry(bid, vno, l.desc, 0, amt, now, user, creditCoa);
            if (l.cogs > 0 && l.coaInv != null && l.coaCogs != null) {
                journalService.insertJournalEntry(bid, vno, l.desc, 0, l.cogs, now, user, l.coaInv);
                journalService.insertJournalEntry(bid, vno, l.desc, l.cogs, 0, now, user, l.coaCogs);
            }
        }
    }

    private Integer whCoaByUnit(Integer unitId) {
        Integer whId = findWarehouseIdByUnit(unitId);
        return whId != null ? findWhCoa(whId) : null;
    }

    private Integer cogsCoaByItem(Integer itemId) {
        return findCogsCoa(itemId);
    }

    private List<JournalLine> getTreatJournalLines(Integer nid) {
        return jdbcTemplate.query("select trx.n_amount_after_disc, treat.v_treatment_name, trx.n_qty "
            + "from tb_treatment_trx trx "
            + "join ms_treatment_fee tf on tf.n_treatment_fee_id = trx.n_treatment_fee_id "
            + "join ms_treatment treat on treat.n_treatment_id = tf.n_treatment_id "
            + "where trx.n_note_id = ? and trx.n_amount_after_disc > 0",
            (rs, rn) -> new JournalLine("TINDAKAN:" + rs.getString("v_treatment_name")
                + ";QTY:" + rs.getInt("n_qty"),
                rs.getDouble("n_amount_after_disc"), 0, null, null), nid);
    }

    private List<JournalLine> getItemJournalLines(Integer nid, Integer coaInvId) {
        return jdbcTemplate.query("select itrx.n_item_id, itrx.n_qty, itrx.n_amount_after_disc "
            + "from tb_item_trx itrx "
            + "where itrx.n_note_id = ? and itrx.n_amount_after_disc > 0",
            (rs, rn) -> {
                Integer iid = rs.getInt("n_item_id");
                double qty = rs.getDouble("n_qty");
                double cogs = calcItemCogs(iid, qty);
                Integer cogsCoa = findCogsCoa(iid);
                return new JournalLine("OBAT:" + iid + ";QTY:" + (int) qty,
                    rs.getDouble("n_amount_after_disc"), cogs, coaInvId, cogsCoa);
            }, nid);
    }

    private List<JournalLine> getMiscJournalLines(Integer nid) {
        return jdbcTemplate.query("select v_misc_name, n_amount_after_disc from tb_misc_trx "
            + "where n_note_id = ? and n_amount_after_disc > 0",
            (rs, rn) -> new JournalLine("MISC:" + rs.getString("v_misc_name") + ";QTY:1",
                rs.getDouble("n_amount_after_disc"), 0, null, null), nid);
    }

    private double calcItemCogs(Integer iid, double qty) {
        try { Double p = jdbcTemplate.queryForObject(
            "select avg(n_last_price * n_last_qty) / nullif(sum(n_last_qty), 0) "
                + "from ms_item where n_item_id = ?", Double.class, iid);
            return p != null ? Math.ceil(p * qty) : 0;
        } catch (EmptyResultDataAccessException e) { return 0; }
    }

    private Integer findWhCoa(Integer whId) {
        try { return jdbcTemplate.queryForObject(
            "select n_coa_id from ms_warehouse where n_whouse_id = ?", Integer.class, whId);
        } catch (EmptyResultDataAccessException e) { return null; }
    }

    private Integer findCogsCoa(Integer iid) {
        try { return jdbcTemplate.queryForObject(
            "select n_coa_cogs from ms_item where n_item_id = ?", Integer.class, iid);
        } catch (EmptyResultDataAccessException e) { return null; }
    }

    private static class JournalLine {
        final String desc; final double amt; final double cogs;
        final Integer coaInv; final Integer coaCogs;
        JournalLine(String d, double a, double c, Integer inv, Integer cc) {
            this.desc = d; this.amt = a; this.cogs = c; this.coaInv = inv; this.coaCogs = cc;
        }
    }

    private List<PolyclinicUnitResponse> getUnits(String username) {
        if (!hasPolyclinicAccess(username)) {
            return new ArrayList<PolyclinicUnitResponse>();
        }

        return jdbcTemplate.query(
            "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                + "from ms_user usr "
                + "join ms_staff staff on staff.n_staff_id = usr.n_staff_id "
                + "join ms_staff_in_unit stfunit on stfunit.n_staff_id = staff.n_staff_id "
                + "join ms_unit unt on unt.n_unit_id = stfunit.n_unit_id "
                + "where upper(usr.v_user_name) = ? and staff.d_staff_fired_date is null "
                + "and unt.unit_type = 1 "
                + "order by unt.v_unit_name",
            (resultSet, rowNum) -> new PolyclinicUnitResponse(
                resultSet.getInt("n_unit_id"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name"),
                getNullableInteger(resultSet, "n_whouse_id")
            ),
            normalizeUpper(username)
        );
    }

    private boolean hasPolyclinicAccess(String username) {
        Integer total = jdbcTemplate.queryForObject(
            "select count(1) from ("
                + "select scr.n_screen_id "
                + "from ms_user usr "
                + "join tb_user_privilege upr on upr.n_user_id = usr.n_user_id "
                + "join ms_screen scr on scr.n_screen_id = upr.n_screen_id "
                + "where upper(usr.v_user_name) = ? and scr.v_screen_code = ? "
                + "union "
                + "select scr.n_screen_id "
                + "from ms_user usr "
                + "join tb_group_privilege gpr on gpr.n_group_id = usr.n_group_id "
                + "join ms_screen scr on scr.n_screen_id = gpr.n_screen_id "
                + "where upper(usr.v_user_name) = ? and scr.v_screen_code = ?"
                + ") screen_access",
            Integer.class,
            normalizeUpper(username),
            SCREEN_POLIKLINIK,
            normalizeUpper(username),
            SCREEN_POLIKLINIK
        );

        return total != null && total.intValue() > 0;
    }

    private List<PolyclinicPatientTypeResponse> getPatientTypes() {
        return jdbcTemplate.query(
            "select n_patient_type_id, v_tpatient, v_tpatient_desc from ms_patient_type order by v_tpatient",
            (resultSet, rowNum) -> new PolyclinicPatientTypeResponse(
                resultSet.getInt("n_patient_type_id"),
                resultSet.getString("v_tpatient"),
                resultSet.getString("v_tpatient_desc")
            )
        );
    }

    private List<PolyclinicEscortResponse> getEscorts() {
        return jdbcTemplate.query(
            "select n_escort_primary_id, v_escort_code, v_escort_type from ms_patient_escort order by v_escort_type",
            (resultSet, rowNum) -> new PolyclinicEscortResponse(
                resultSet.getInt("n_escort_primary_id"),
                resultSet.getString("v_escort_code"),
                resultSet.getString("v_escort_type")
            )
        );
    }

    private void persistLines(
        Integer noteId,
        UnitRow unit,
        List<PolyclinicLineItemRequest> lines,
        Integer defaultDoctorId,
        String username,
        Timestamp now
    ) {
        for (PolyclinicLineItemRequest line : lines) {
            String type = normalizeUpper(line.getLineType());
            if ("TREATMENT".equals(type)) {
                persistTreatmentLine(noteId, line, defaultDoctorId, username, now);
            } else if ("ITEM".equals(type)) {
                persistItemLine(noteId, unit, line, username, now);
            } else if ("MISC".equals(type)) {
                persistMiscLine(noteId, line, username, now);
            } else {
                throw new IllegalArgumentException("Jenis baris transaksi tidak dikenali: " + line.getLineType());
            }
        }
    }

    private void persistTreatmentLine(
        Integer noteId,
        PolyclinicLineItemRequest line,
        Integer defaultDoctorId,
        String username,
        Timestamp now
    ) {
        TreatmentFeeRow treatment = findTreatmentFee(line.getReferenceId());
        short quantity = toShort(line.getQuantity());
        double unitPrice = treatment.price;
        double amount = unitPrice * quantity;
        double discountAmount = calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
        double subtotal = amount - discountAmount;
        Integer doctorId = line.getDoctorStaffId() != null ? line.getDoctorStaffId() : defaultDoctorId;

        Integer trxId = nextSequenceValue("tb_treatment_trx_n_treatment_id_seq");
        jdbcTemplate.update(
            "insert into tb_treatment_trx (n_treatment_id, n_note_id, n_doctor_id, n_treatment_fee_id, n_amount_trx, "
                + "n_qty, v_who_create, d_whn_create, v_disc_type, n_disc_amount, n_amount_after_disc) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            trxId,
            noteId,
            doctorId,
            treatment.treatmentFeeId,
            amount,
            quantity,
            normalizeUpper(username),
            now,
            normalizeDiscountType(line.getDiscountType()),
            discountAmount,
            subtotal
        );
    }

    private void persistItemLine(
        Integer noteId,
        UnitRow unit,
        PolyclinicLineItemRequest line,
        String username,
        Timestamp now
    ) {
        ItemRow item = findItem(line.getReferenceId(), unit.unitId);
        double quantity = line.getQuantity().doubleValue();
        double amount = item.price * quantity;
        double discountAmount = calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
        double subtotal = amount - discountAmount;

        double remaining = quantity;
        double perUnitDiscount = quantity == 0 ? 0 : discountAmount / quantity;
        double perUnitSubtotal = quantity == 0 ? 0 : subtotal / quantity;

        List<InventoryRow> inventories = findInventories(unit.warehouseId, item.itemId);
        for (InventoryRow inventory : inventories) {
            if (remaining <= 0) {
                break;
            }

            double picked = Math.min(remaining, inventory.quantity);
            Integer trxId = nextSequenceValue("tb_item_trx_n_item_trx_id_seq");
            jdbcTemplate.update(
                "insert into tb_item_trx (n_item_trx_id, n_item_id, n_batch_id, n_note_id, n_amount_trx, n_qty, "
                    + "v_who_create, d_whn_create, v_disc_type, n_disc_amount, n_amount_after_disc, aturan_pakai) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                trxId,
                item.itemId,
                inventory.batchId,
                noteId,
                item.price * picked,
                picked,
                normalizeUpper(username),
                now,
                normalizeDiscountType(line.getDiscountType()),
                perUnitDiscount * picked,
                perUnitSubtotal * picked,
                line.getInstruction()
            );

            jdbcTemplate.update(
                "update tb_item_inventory set n_item_inv_qty = ? where n_item_id = ? and n_batch_id = ? and n_whouse_id = ?",
                inventory.quantity - picked,
                item.itemId,
                inventory.batchId,
                unit.warehouseId
            );

            remaining -= picked;
        }

        if (remaining > 0) {
            throw new IllegalStateException("Stok item " + item.itemCode + " tidak mencukupi.");
        }
    }

    private void persistMiscLine(Integer noteId, PolyclinicLineItemRequest line, String username, Timestamp now) {
        short quantity = toShort(line.getQuantity());
        double unitPrice = requireNonNegative(line.getUnitPrice(), "Harga misc wajib diisi.");
        double amount = unitPrice * quantity;
        double discountAmount = calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
        double subtotal = amount - discountAmount;

        Integer trxId = nextSequenceValue("tb_misc_trx_n_misc_trx_id_seq");
        jdbcTemplate.update(
            "insert into tb_misc_trx (n_misc_trx_id, n_note_id, n_amount_trx, n_qty, v_who_create, d_whn_create, "
                + "n_disc_amount, v_disc_type, n_amount_after_disc, n_item_price, v_misc_name) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            trxId,
            noteId,
            amount,
            quantity,
            normalizeUpper(username),
            now,
            discountAmount,
            normalizeDiscountType(line.getDiscountType()),
            subtotal,
            unitPrice,
            normalizeUpper(line.getDescription())
        );
    }

    private void restoreInventoryForItems(Integer noteId, Integer unitId) {
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        if (warehouseId == null) {
            return;
        }

        List<InventoryRestoreRow> itemRows = jdbcTemplate.query(
            "select n_item_id, n_batch_id, n_qty from tb_item_trx where n_note_id = ?",
            (resultSet, rowNum) -> new InventoryRestoreRow(
                resultSet.getInt("n_item_id"),
                resultSet.getInt("n_batch_id"),
                resultSet.getDouble("n_qty")
            ),
            noteId
        );

        for (InventoryRestoreRow row : itemRows) {
            restoreInventory(row.itemId, row.batchId, warehouseId, row.quantity);
        }
    }

    private void restoreInventoryForCancel(Integer noteId, Integer unitId) {
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        if (warehouseId != null) {
            restoreInventoryForItems(noteId, unitId);
            List<InventoryRestoreRow> bundledRows = jdbcTemplate.query(
                "select bitrx.n_item_id, bitrx.n_batch_id, bitrx.n_qty "
                    + "from tb_bundled_item_used_trx bitrx "
                    + "join tb_bundled_trx btrx on btrx.n_bundled_trx_id = bitrx.n_tbundled_id "
                    + "where btrx.n_note_id = ?",
                (resultSet, rowNum) -> new InventoryRestoreRow(
                    resultSet.getInt("n_item_id"),
                    resultSet.getInt("n_batch_id"),
                    resultSet.getDouble("n_qty")
                ),
                noteId
            );
            for (InventoryRestoreRow row : bundledRows) {
                restoreInventory(row.itemId, row.batchId, warehouseId, row.quantity);
            }
        }
    }

    private void restoreInventory(Integer itemId, Integer batchId, Integer warehouseId, double quantity) {
        Double currentQty = jdbcTemplate.queryForObject(
            "select n_item_inv_qty from tb_item_inventory where n_item_id = ? and n_batch_id = ? and n_whouse_id = ?",
            Double.class,
            itemId,
            batchId,
            warehouseId
        );
        if (currentQty == null) {
            throw new IllegalStateException("Inventory item/batch tidak ditemukan saat rollback stok.");
        }
        jdbcTemplate.update(
            "update tb_item_inventory set n_item_inv_qty = ? where n_item_id = ? and n_batch_id = ? and n_whouse_id = ?",
            currentQty + quantity,
            itemId,
            batchId,
            warehouseId
        );
    }

    private void deleteNoteLineTables(Integer noteId) {
        jdbcTemplate.update("delete from tb_item_trx where n_note_id = ?", noteId);
        jdbcTemplate.update("delete from tb_treatment_trx where n_note_id = ?", noteId);
        jdbcTemplate.update("delete from tb_misc_trx where n_note_id = ?", noteId);
    }

    private void validateSaveRequest(PolyclinicSaveRequest request) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("Minimal satu baris transaksi poliklinik harus diisi.");
        }
        if (Boolean.TRUE.equals(request.getReferencePatient())) {
            if (!hasText(request.getPatientName()) || !hasText(request.getBirthDate()) || !hasText(request.getAddress())) {
                throw new IllegalArgumentException("Pasien bebas wajib mengisi nama, tanggal lahir, dan alamat.");
            }
        } else if (!hasText(request.getExistingMrCode())) {
            throw new IllegalArgumentException("Pasien registrasi wajib memilih No.MR.");
        }
    }

    private double calculateHeaderTotal(List<PolyclinicLineItemRequest> lines) {
        double total = 0;
        for (PolyclinicLineItemRequest line : lines) {
            String type = normalizeUpper(line.getLineType());
            if ("TREATMENT".equals(type)) {
                TreatmentFeeRow treatment = findTreatmentFee(line.getReferenceId());
                double amount = treatment.price * line.getQuantity().doubleValue();
                total += amount - calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
            } else if ("ITEM".equals(type)) {
                Integer unitId = null;
                if (line.getContextUnitId() != null) {
                    unitId = line.getContextUnitId();
                }
                if (unitId == null) {
                    throw new IllegalArgumentException("Konteks unit item tidak tersedia.");
                }
                ItemRow item = findItem(line.getReferenceId(), unitId);
                double amount = item.price * line.getQuantity().doubleValue();
                total += amount - calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
            } else if ("MISC".equals(type)) {
                double amount = requireNonNegative(line.getUnitPrice(), "Harga misc wajib diisi.") * line.getQuantity().doubleValue();
                total += amount - calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
            }
        }
        return total;
    }

    private List<PolyclinicNoteLineResponse> getTreatmentLines(Integer noteId) {
        return jdbcTemplate.query(
            "select trx.n_treatment_id, treat.v_treatment_code, treat.v_treatment_name, trx.n_qty, trx.n_amount_trx, "
                + "trx.n_disc_amount, trx.n_amount_after_disc, trx.v_disc_type, doc.n_staff_id as doctor_id, "
                + "doc.v_staff_name as doctor_name, coalesce(tfee.n_doctor_fee, 0) as doctor_fee, tfee.n_treatment_fee_id "
                + "from tb_treatment_trx trx "
                + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                + "left join ms_staff doc on doc.n_staff_id = trx.n_doctor_id "
                + "where trx.n_note_id = ? "
                + "order by trx.n_treatment_id",
            (resultSet, rowNum) -> {
                String description = resultSet.getString("v_treatment_name");
                if (resultSet.getString("doctor_name") != null && resultSet.getDouble("doctor_fee") > 0) {
                    description = description + " - " + resultSet.getString("doctor_name");
                }
                double quantity = resultSet.getDouble("n_qty");
                double amount = resultSet.getDouble("n_amount_trx");
                return new PolyclinicNoteLineResponse(
                    "TREATMENT",
                    resultSet.getInt("n_treatment_id"),
                    resultSet.getInt("n_treatment_fee_id"),
                    resultSet.getString("v_treatment_code"),
                    description,
                    quantity,
                    "-",
                    quantity == 0 ? 0 : amount / quantity,
                    normalizeDiscountType(resultSet.getString("v_disc_type")),
                    resultSet.getDouble("n_disc_amount"),
                    resultSet.getDouble("n_amount_after_disc"),
                    getNullableInteger(resultSet, "doctor_id"),
                    resultSet.getString("doctor_name"),
                    null
                );
            },
            noteId
        );
    }

    private List<PolyclinicNoteLineResponse> getItemLines(Integer noteId) {
        return jdbcTemplate.query(
            "select trx.n_item_id as id, item.v_item_code as code, item.v_item_name as name, sat.v_mitem_end_quantify as satuan, "
                + "sum(trx.n_qty) as qty, sum(trx.n_amount_trx) as value, sum(trx.n_disc_amount) as discount, "
                + "sum(trx.n_amount_after_disc) as total, max(trx.v_disc_type) as disc_type, max(trx.aturan_pakai) as aturan "
                + "from tb_item_trx trx, ms_item item, ms_item_measurement sat "
                + "where trx.n_item_id = item.n_item_id "
                + "and item.n_mitem_id = sat.n_mitem_id "
                + "and trx.n_note_id = ? "
                + "group by id, code, name, satuan "
                + "order by name",
            (resultSet, rowNum) -> {
                double quantity = resultSet.getDouble("qty");
                double amount = resultSet.getDouble("value");
                return new PolyclinicNoteLineResponse(
                    "ITEM",
                    resultSet.getInt("id"),
                    resultSet.getInt("id"),
                    resultSet.getString("code"),
                    resultSet.getString("name"),
                    quantity,
                    resultSet.getString("satuan"),
                    quantity == 0 ? 0 : amount / quantity,
                    normalizeDiscountType(resultSet.getString("disc_type")),
                    resultSet.getDouble("discount"),
                    resultSet.getDouble("total"),
                    null,
                    null,
                    resultSet.getString("aturan")
                );
            },
            noteId
        );
    }

    private List<PolyclinicNoteLineResponse> getMiscLines(Integer noteId) {
        return jdbcTemplate.query(
            "select n_misc_trx_id, v_misc_name, n_qty, n_item_price, n_disc_amount, v_disc_type, n_amount_after_disc "
                + "from tb_misc_trx where n_note_id = ? order by n_misc_trx_id",
            (resultSet, rowNum) -> new PolyclinicNoteLineResponse(
                "MISC",
                resultSet.getInt("n_misc_trx_id"),
                resultSet.getInt("n_misc_trx_id"),
                "MISC-001",
                resultSet.getString("v_misc_name"),
                resultSet.getDouble("n_qty"),
                "-",
                resultSet.getDouble("n_item_price"),
                normalizeDiscountType(resultSet.getString("v_disc_type")),
                resultSet.getDouble("n_disc_amount"),
                resultSet.getDouble("n_amount_after_disc"),
                null,
                null,
                null
            ),
            noteId
        );
    }

    private List<PolyclinicNoteLineResponse> getBundleLines(Integer noteId) {
        return jdbcTemplate.query(
            "select btrx.n_bundled_trx_id, tfee.n_treatment_fee_id, treat.v_treatment_code, treat.v_treatment_name, "
                + "btrx.n_qty, btrx.n_amount_trx, btrx.n_disc_amount, btrx.n_amount_after_disc, btrx.v_disc_type "
                + "from tb_bundled_trx btrx "
                + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = btrx.n_tbundled_id "
                + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                + "where btrx.n_note_id = ? "
                + "order by btrx.n_bundled_trx_id",
            (resultSet, rowNum) -> {
                double quantity = resultSet.getDouble("n_qty");
                double amount = resultSet.getDouble("n_amount_trx");
                return new PolyclinicNoteLineResponse(
                    "BUNDLE",
                    resultSet.getInt("n_bundled_trx_id"),
                    resultSet.getInt("n_treatment_fee_id"),
                    resultSet.getString("v_treatment_code"),
                    resultSet.getString("v_treatment_name"),
                    quantity,
                    "-",
                    quantity == 0 ? 0 : amount / quantity,
                    normalizeDiscountType(resultSet.getString("v_disc_type")),
                    resultSet.getDouble("n_disc_amount"),
                    resultSet.getDouble("n_amount_after_disc"),
                    null,
                    null,
                    null
                );
            },
            noteId
        );
    }

    private boolean hasBundleLines(Integer noteId) {
        Integer count = jdbcTemplate.queryForObject(
            "select count(*) from tb_bundled_trx where n_note_id = ?",
            Integer.class,
            noteId
        );
        return count != null && count.intValue() > 0;
    }

    private PolyclinicNoteHeader findNoteHeader(Integer noteId) {
        try {
            return jdbcTemplate.queryForObject(
                "select note.n_exam_id, note.v_note_no, note.n_exam_status, note.n_total_amount, note.n_unit_id, "
                    + "unit.v_unit_code, unit.v_unit_name, note.n_patient_id, patient.n_patient_type_id, patient.v_patient_name, "
                    + "patient.v_patient_gender, patient.d_patient_dob, patient.v_patient_main_addr, note.n_escort_id, "
                    + "note.v_cancelation_note, reg.n_reg_id, reg.v_reg_secondary_id, mr.v_mr_code, "
                    + "doc.n_staff_id as doctor_id, doc.v_staff_code as doctor_code, doc.v_staff_name as doctor_name "
                    + "from tb_examination note "
                    + "join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                    + "join ms_patient patient on patient.n_patient_id = note.n_patient_id "
                    + "left join tb_registration reg on reg.n_reg_id = note.n_reg_id "
                    + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                    + "left join ms_staff doc on doc.n_staff_id = reg.n_staff_id "
                    + "where note.n_exam_id = ?",
                (resultSet, rowNum) -> new PolyclinicNoteHeader(
                    resultSet.getInt("n_exam_id"),
                    resultSet.getString("v_note_no"),
                    resultSet.getInt("n_exam_status"),
                    resultSet.getDouble("n_total_amount"),
                    resultSet.getInt("n_unit_id"),
                    resultSet.getString("v_unit_code"),
                    resultSet.getString("v_unit_name"),
                    resultSet.getInt("n_patient_id"),
                    getNullableInteger(resultSet, "n_patient_type_id"),
                    resultSet.getString("v_patient_name"),
                    resultSet.getString("v_patient_gender"),
                    toIsoDate(resultSet.getDate("d_patient_dob")),
                    resultSet.getString("v_patient_main_addr"),
                    resultSet.getString("v_mr_code"),
                    getNullableInteger(resultSet, "n_reg_id"),
                    resultSet.getString("v_reg_secondary_id"),
                    getNullableInteger(resultSet, "doctor_id"),
                    resultSet.getString("doctor_code"),
                    resultSet.getString("doctor_name"),
                    getNullableInteger(resultSet, "n_escort_id"),
                    resultSet.getString("v_cancelation_note")
                ),
                noteId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Nota poliklinik tidak ditemukan.");
        }
    }

    private RegisteredContext findRegisteredContext(Integer unitId, String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("No.MR pasien registrasi wajib dipilih.");
        }
        try {
            return jdbcTemplate.queryForObject(
                "select mr.n_mr_id, mr.v_mr_code, mr.n_patient_id, reg.n_reg_id, reg.v_reg_secondary_id, reg.n_staff_id "
                    + "from tb_medical_record mr "
                    + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                    + "where reg.reg_status = ? and reg.n_unit_id = ? and mr.v_mr_code = ? "
                    + "order by reg.d_registration_date desc limit 1",
                (resultSet, rowNum) -> new RegisteredContext(
                    resultSet.getInt("n_mr_id"),
                    resultSet.getString("v_mr_code"),
                    resultSet.getInt("n_patient_id"),
                    resultSet.getInt("n_reg_id"),
                    resultSet.getString("v_reg_secondary_id"),
                    getNullableInteger(resultSet, "n_staff_id")
                ),
                REG_ACTIVE,
                unitId,
                normalizeMrCode(mrCode)
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Registrasi pasien aktif tidak ditemukan pada unit poliklinik.");
        }
    }

    private UnitRow findUnit(Integer unitId) {
        try {
            return jdbcTemplate.queryForObject(
                "select n_unit_id, v_unit_code, v_unit_name, n_whouse_id from ms_unit where n_unit_id = ?",
                (resultSet, rowNum) -> new UnitRow(
                    resultSet.getInt("n_unit_id"),
                    resultSet.getString("v_unit_code"),
                    resultSet.getString("v_unit_name"),
                    getNullableInteger(resultSet, "n_whouse_id")
                ),
                unitId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Unit poliklinik tidak ditemukan.");
        }
    }

    private Integer findWarehouseIdByUnit(Integer unitId) {
        try {
            return jdbcTemplate.queryForObject(
                "select n_whouse_id from ms_unit where n_unit_id = ?",
                Integer.class,
                unitId
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private TreatmentFeeRow findTreatmentFee(Integer treatmentFeeId) {
        try {
            return jdbcTemplate.queryForObject(
                "select n_treatment_fee_id, coalesce(n_trtfee_fee, 0) as n_trtfee_fee from ms_treatment_fee where n_treatment_fee_id = ?",
                (resultSet, rowNum) -> new TreatmentFeeRow(resultSet.getInt("n_treatment_fee_id"), resultSet.getDouble("n_trtfee_fee")),
                treatmentFeeId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Master tindakan tidak ditemukan.");
        }
    }

    private ItemRow findItem(Integer itemId, Integer unitId) {
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        if (warehouseId == null) {
            throw new IllegalStateException("Gudang unit tidak ditemukan.");
        }
        try {
            return jdbcTemplate.queryForObject(
                "select item.n_item_id, item.v_item_code, item.v_item_name, sell.n_selling_price "
                    + "from ms_item item "
                    + "join ms_item_selling_price sell on sell.n_item_id = item.n_item_id "
                    + "join ms_treatment_class tclass on tclass.n_tclass_id = sell.n_tclass_id "
                    + "where item.n_item_id = ? "
                    + "and exists (select 1 from tb_item_inventory inv where inv.n_whouse_id = ? and inv.n_item_id = item.n_item_id and inv.n_item_inv_qty > 0) "
                    + "and tclass.v_tclass_desc = ? "
                    + "limit 1",
                (resultSet, rowNum) -> new ItemRow(
                    resultSet.getInt("n_item_id"),
                    resultSet.getString("v_item_code"),
                    resultSet.getString("v_item_name"),
                    resultSet.getDouble("n_selling_price")
                ),
                itemId,
                warehouseId,
                DEFAULT_TARIFF_CLASS
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Master item tidak ditemukan atau stok tidak tersedia.");
        }
    }

    private List<InventoryRow> findInventories(Integer warehouseId, Integer itemId) {
        return jdbcTemplate.query(
            "select n_batch_id, n_item_inv_qty from tb_item_inventory "
                + "where n_whouse_id = ? and n_item_id = ? and n_item_inv_qty > 0 "
                + "order by n_batch_id",
            (resultSet, rowNum) -> new InventoryRow(
                resultSet.getInt("n_batch_id"),
                resultSet.getDouble("n_item_inv_qty")
            ),
            warehouseId,
            itemId
        );
    }

    private void insertReferencePatient(Integer patientId, PolyclinicSaveRequest request, String username, Timestamp now) {
        jdbcTemplate.update(
            "insert into ms_patient (n_patient_id, n_patient_type_id, v_patient_name, v_patient_gender, d_patient_dob, "
                + "v_patient_main_addr, v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?)",
            patientId,
            request.getPatientTypeId(),
            normalizeUpper(request.getPatientName()),
            normalizeGender(request.getGender()),
            Date.valueOf(LocalDate.parse(request.getBirthDate(), ISO_DATE)),
            normalizeUpper(request.getAddress()),
            normalizeUpper(username),
            now
        );
    }

    private void updateReferencePatient(Integer patientId, PolyclinicSaveRequest request, String username, Timestamp now) {
        jdbcTemplate.update(
            "update ms_patient set n_patient_type_id = ?, v_patient_name = ?, v_patient_gender = ?, d_patient_dob = ?, "
                + "v_patient_main_addr = ?, v_who_change = ?, d_whn_change = ? where n_patient_id = ?",
            request.getPatientTypeId(),
            normalizeUpper(request.getPatientName()),
            normalizeGender(request.getGender()),
            Date.valueOf(LocalDate.parse(request.getBirthDate(), ISO_DATE)),
            normalizeUpper(request.getAddress()),
            normalizeUpper(username),
            now,
            patientId
        );
    }

    private Integer nextSequenceValue(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private String generateNotaNumber(Integer noteSequence, Timestamp timestamp, String unitCode) {
        return "J-" + unitCode + "-" + NOTE_DATE_FORMAT.format(timestamp) + "-" + formatNoteSequence(noteSequence);
    }

    private String formatNoteSequence(Integer sequence) {
        return String.format(Locale.ROOT, "%06d", sequence);
    }

    private String toIsoDate(Date date) {
        return date == null ? null : date.toLocalDate().format(ISO_DATE);
    }

    private String toStatusLabel(int status) {
        if (status == NOTE_ACTIVE) {
            return "BARU";
        }
        if (status == NOTE_CANCELED) {
            return "BATAL";
        }
        if (status == NOTE_VALIDATED) {
            return "SUDAH DIVALIDASI";
        }
        if (status == NOTE_VALIDATED_CANCELED) {
            return "SUDAH DIVALIDASI TAPI DIBATALKAN";
        }
        return "TIDAK AKTIF";
    }

    private double calculateDiscount(double amount, String discountType, Double discountValue) {
        double value = discountValue == null ? 0 : discountValue.doubleValue();
        if (value <= 0) {
            return 0;
        }
        String normalizedType = normalizeDiscountType(discountType);
        if (DISCOUNT_PERCENT.equals(normalizedType)) {
            return amount * value / 100d;
        }
        return value;
    }

    private String normalizeDiscountType(String discountType) {
        return DISCOUNT_PERCENT.equals(discountType) ? DISCOUNT_PERCENT : DISCOUNT_RP;
    }

    private String normalizeTariffClass(String tariffClass) {
        return hasText(tariffClass) ? tariffClass.trim().toUpperCase(Locale.ROOT) : DEFAULT_TARIFF_CLASS;
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeGender(String gender) {
        return "F".equalsIgnoreCase(gender) ? "F" : "M";
    }

    private String normalizeMrCode(String mrCode) {
        String value = normalizeUpper(mrCode);
        if (value == null) {
            return null;
        }
        if (value.length() == 6 && value.matches("\\d{6}")) {
            return value.substring(0, 2) + "-" + value.substring(2, 4) + "-" + value.substring(4, 6);
        }
        return value;
    }

    private String likeText(String value) {
        return "%" + (value == null ? "" : value.trim()) + "%";
    }

    private String likeUpper(String value) {
        return "%" + (value == null ? "" : value.trim().toUpperCase(Locale.ROOT)) + "%";
    }

    private String likeRaw(String value) {
        return "%" + (value == null ? "" : value.trim()) + "%";
    }

    private String likeMrCode(String mrCode) {
        String normalized = normalizeMrCode(mrCode);
        return "%" + (normalized == null ? "" : normalized) + "%";
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private Short getNullableShort(ResultSet resultSet, String columnName) throws SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.shortValue();
    }

    private short toShort(Number number) {
        if (number == null) {
            throw new IllegalArgumentException("Jumlah transaksi wajib diisi.");
        }
        if (number.doubleValue() <= 0) {
            throw new IllegalArgumentException("Jumlah transaksi harus lebih besar dari nol.");
        }
        return (short) number.intValue();
    }

    private double requireNonNegative(Double value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value.doubleValue() < 0) {
            throw new IllegalArgumentException(message);
        }
        return value.doubleValue();
    }

    private static final class UnitRow {
        private final Integer unitId;
        private final String unitCode;
        private final String unitName;
        private final Integer warehouseId;

        private UnitRow(Integer unitId, String unitCode, String unitName, Integer warehouseId) {
            this.unitId = unitId;
            this.unitCode = unitCode;
            this.unitName = unitName;
            this.warehouseId = warehouseId;
        }
    }

    private static final class RegisteredContext {
        private final Integer mrId;
        private final String mrCode;
        private final Integer patientId;
        private final Integer registrationId;
        private final String registrationCode;
        private final Integer doctorId;

        private RegisteredContext(
            Integer mrId,
            String mrCode,
            Integer patientId,
            Integer registrationId,
            String registrationCode,
            Integer doctorId
        ) {
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientId = patientId;
            this.registrationId = registrationId;
            this.registrationCode = registrationCode;
            this.doctorId = doctorId;
        }
    }

    private static final class TreatmentFeeRow {
        private final Integer treatmentFeeId;
        private final double price;

        private TreatmentFeeRow(Integer treatmentFeeId, double price) {
            this.treatmentFeeId = treatmentFeeId;
            this.price = price;
        }
    }

    private static final class ItemRow {
        private final Integer itemId;
        private final String itemCode;
        private final String itemName;
        private final double price;

        private ItemRow(Integer itemId, String itemCode, String itemName, double price) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.price = price;
        }
    }

    private static final class InventoryRow {
        private final Integer batchId;
        private final double quantity;

        private InventoryRow(Integer batchId, double quantity) {
            this.batchId = batchId;
            this.quantity = quantity;
        }
    }

    private static final class InventoryRestoreRow {
        private final Integer itemId;
        private final Integer batchId;
        private final double quantity;

        private InventoryRestoreRow(Integer itemId, Integer batchId, double quantity) {
            this.itemId = itemId;
            this.batchId = batchId;
            this.quantity = quantity;
        }
    }

    private static final class PolyclinicNoteHeader {
        private final Integer noteId;
        private final String noteNumber;
        private final int statusCode;
        private final double totalAmount;
        private final Integer unitId;
        private final String unitCode;
        private final String unitName;
        private final Integer patientId;
        private final Integer patientTypeId;
        private final String patientName;
        private final String gender;
        private final String birthDate;
        private final String address;
        private final String medicalRecordCode;
        private final Integer registrationId;
        private final String registrationCode;
        private final Integer doctorId;
        private final String doctorCode;
        private final String doctorName;
        private final Integer escortId;
        private final String cancelationNote;

        private PolyclinicNoteHeader(
            Integer noteId,
            String noteNumber,
            int statusCode,
            double totalAmount,
            Integer unitId,
            String unitCode,
            String unitName,
            Integer patientId,
            Integer patientTypeId,
            String patientName,
            String gender,
            String birthDate,
            String address,
            String medicalRecordCode,
            Integer registrationId,
            String registrationCode,
            Integer doctorId,
            String doctorCode,
            String doctorName,
            Integer escortId,
            String cancelationNote
        ) {
            this.noteId = noteId;
            this.noteNumber = noteNumber;
            this.statusCode = statusCode;
            this.totalAmount = totalAmount;
            this.unitId = unitId;
            this.unitCode = unitCode;
            this.unitName = unitName;
            this.patientId = patientId;
            this.patientTypeId = patientTypeId;
            this.patientName = patientName;
            this.gender = gender;
            this.birthDate = birthDate;
            this.address = address;
            this.medicalRecordCode = medicalRecordCode;
            this.registrationId = registrationId;
            this.registrationCode = registrationCode;
            this.doctorId = doctorId;
            this.doctorCode = doctorCode;
            this.doctorName = doctorName;
            this.escortId = escortId;
            this.cancelationNote = cancelationNote;
        }
    }
}

class PolyclinicMastersResponse {
    private final List<PolyclinicUnitResponse> units;
    private final List<PolyclinicPatientTypeResponse> patientTypes;
    private final List<PolyclinicEscortResponse> escorts;

    PolyclinicMastersResponse(
        List<PolyclinicUnitResponse> units,
        List<PolyclinicPatientTypeResponse> patientTypes,
        List<PolyclinicEscortResponse> escorts
    ) {
        this.units = units;
        this.patientTypes = patientTypes;
        this.escorts = escorts;
    }

    public List<PolyclinicUnitResponse> getUnits() {
        return units;
    }

    public List<PolyclinicPatientTypeResponse> getPatientTypes() {
        return patientTypes;
    }

    public List<PolyclinicEscortResponse> getEscorts() {
        return escorts;
    }
}

class PolyclinicUnitResponse {
    private final Integer unitId;
    private final String unitCode;
    private final String unitName;
    private final Integer warehouseId;

    PolyclinicUnitResponse(Integer unitId, String unitCode, String unitName, Integer warehouseId) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.warehouseId = warehouseId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }
}

class PolyclinicPatientTypeResponse {
    private final Integer patientTypeId;
    private final String patientTypeCode;
    private final String patientTypeName;

    PolyclinicPatientTypeResponse(Integer patientTypeId, String patientTypeCode, String patientTypeName) {
        this.patientTypeId = patientTypeId;
        this.patientTypeCode = patientTypeCode;
        this.patientTypeName = patientTypeName;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientTypeCode() {
        return patientTypeCode;
    }

    public String getPatientTypeName() {
        return patientTypeName;
    }
}

class PolyclinicEscortResponse {
    private final Integer escortId;
    private final String escortCode;
    private final String escortType;

    PolyclinicEscortResponse(Integer escortId, String escortCode, String escortType) {
        this.escortId = escortId;
        this.escortCode = escortCode;
        this.escortType = escortType;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public String getEscortCode() {
        return escortCode;
    }

    public String getEscortType() {
        return escortType;
    }
}

class PolyclinicRegisteredPatientResponse {
    private final Integer medicalRecordId;
    private final String medicalRecordCode;
    private final String patientName;
    private final String address;

    PolyclinicRegisteredPatientResponse(Integer medicalRecordId, String medicalRecordCode, String patientName, String address) {
        this.medicalRecordId = medicalRecordId;
        this.medicalRecordCode = medicalRecordCode;
        this.patientName = patientName;
        this.address = address;
    }

    public Integer getMedicalRecordId() {
        return medicalRecordId;
    }

    public String getMedicalRecordCode() {
        return medicalRecordCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getAddress() {
        return address;
    }
}

class PolyclinicPatientDetailResponse {
    private final Integer patientId;
    private final Integer medicalRecordId;
    private final String medicalRecordCode;
    private final Integer registrationId;
    private final String registrationCode;
    private final Integer patientTypeId;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String address;
    private final Integer doctorId;
    private final String doctorCode;
    private final String doctorName;
    private final String registrationDateTime;

    PolyclinicPatientDetailResponse(
        Integer patientId,
        Integer medicalRecordId,
        String medicalRecordCode,
        Integer registrationId,
        String registrationCode,
        Integer patientTypeId,
        String patientName,
        String gender,
        String birthDate,
        String address,
        Integer doctorId,
        String doctorCode,
        String doctorName,
        String registrationDateTime
    ) {
        this.patientId = patientId;
        this.medicalRecordId = medicalRecordId;
        this.medicalRecordCode = medicalRecordCode;
        this.registrationId = registrationId;
        this.registrationCode = registrationCode;
        this.patientTypeId = patientTypeId;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.doctorId = doctorId;
        this.doctorCode = doctorCode;
        this.doctorName = doctorName;
        this.registrationDateTime = registrationDateTime;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public Integer getMedicalRecordId() {
        return medicalRecordId;
    }

    public String getMedicalRecordCode() {
        return medicalRecordCode;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getRegistrationDateTime() {
        return registrationDateTime;
    }
}

class PolyclinicDoctorResponse {
    private final Integer doctorId;
    private final String doctorCode;
    private final String doctorName;

    PolyclinicDoctorResponse(Integer doctorId, String doctorCode, String doctorName) {
        this.doctorId = doctorId;
        this.doctorCode = doctorCode;
        this.doctorName = doctorName;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public String getDoctorName() {
        return doctorName;
    }
}

class PolyclinicTreatmentOptionResponse {
    private final Integer treatmentFeeId;
    private final Integer treatmentId;
    private final String treatmentCode;
    private final String treatmentName;
    private final double price;
    private final double doctorFee;

    PolyclinicTreatmentOptionResponse(
        Integer treatmentFeeId,
        Integer treatmentId,
        String treatmentCode,
        String treatmentName,
        double price,
        double doctorFee
    ) {
        this.treatmentFeeId = treatmentFeeId;
        this.treatmentId = treatmentId;
        this.treatmentCode = treatmentCode;
        this.treatmentName = treatmentName;
        this.price = price;
        this.doctorFee = doctorFee;
    }

    public Integer getTreatmentFeeId() {
        return treatmentFeeId;
    }

    public Integer getTreatmentId() {
        return treatmentId;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public double getPrice() {
        return price;
    }

    public double getDoctorFee() {
        return doctorFee;
    }
}

class PolyclinicItemOptionResponse {
    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final String unitName;
    private final double price;
    private final double stockQuantity;
    private final Short jasaR;
    private final Short itemType;

    PolyclinicItemOptionResponse(
        Integer itemId,
        String itemCode,
        String itemName,
        String unitName,
        double price,
        double stockQuantity,
        Short jasaR,
        Short itemType
    ) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unitName = unitName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.jasaR = jasaR;
        this.itemType = itemType;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getUnitName() {
        return unitName;
    }

    public double getPrice() {
        return price;
    }

    public double getStockQuantity() {
        return stockQuantity;
    }

    public Short getJasaR() {
        return jasaR;
    }

    public Short getItemType() {
        return itemType;
    }
}

class PolyclinicNoteSummaryResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final String patientName;
    private final Integer statusCode;
    private final String statusLabel;
    private final String createdAt;

    PolyclinicNoteSummaryResponse(
        Integer noteId,
        String noteNumber,
        String patientName,
        Integer statusCode,
        String statusLabel,
        String createdAt
    ) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.patientName = patientName;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.createdAt = createdAt;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

class PolyclinicNoteDetailResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final Integer statusCode;
    private final String statusLabel;
    private final double totalAmount;
    private final Integer unitId;
    private final String unitCode;
    private final String unitName;
    private final Integer patientId;
    private final Integer patientTypeId;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String address;
    private final String medicalRecordCode;
    private final Integer registrationId;
    private final String registrationCode;
    private final Integer doctorId;
    private final String doctorCode;
    private final String doctorName;
    private final Integer escortId;
    private final String cancelationNote;
    private final boolean canModify;
    private final boolean canValidate;
    private final boolean canCancel;
    private final List<PolyclinicNoteLineResponse> lines;

    PolyclinicNoteDetailResponse(
        Integer noteId,
        String noteNumber,
        Integer statusCode,
        String statusLabel,
        double totalAmount,
        Integer unitId,
        String unitCode,
        String unitName,
        Integer patientId,
        Integer patientTypeId,
        String patientName,
        String gender,
        String birthDate,
        String address,
        String medicalRecordCode,
        Integer registrationId,
        String registrationCode,
        Integer doctorId,
        String doctorCode,
        String doctorName,
        Integer escortId,
        String cancelationNote,
        boolean canModify,
        boolean canValidate,
        boolean canCancel,
        List<PolyclinicNoteLineResponse> lines
    ) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.totalAmount = totalAmount;
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.patientId = patientId;
        this.patientTypeId = patientTypeId;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.medicalRecordCode = medicalRecordCode;
        this.registrationId = registrationId;
        this.registrationCode = registrationCode;
        this.doctorId = doctorId;
        this.doctorCode = doctorCode;
        this.doctorName = doctorName;
        this.escortId = escortId;
        this.cancelationNote = cancelationNote;
        this.canModify = canModify;
        this.canValidate = canValidate;
        this.canCancel = canCancel;
        this.lines = lines;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }

    public String getMedicalRecordCode() {
        return medicalRecordCode;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public String getCancelationNote() {
        return cancelationNote;
    }

    public boolean isCanModify() {
        return canModify;
    }

    public boolean isCanValidate() {
        return canValidate;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public List<PolyclinicNoteLineResponse> getLines() {
        return lines;
    }
}

class PolyclinicNoteLineResponse {
    private final String lineType;
    private final Integer lineId;
    private final Integer referenceId;
    private final String code;
    private final String description;
    private final double quantity;
    private final String unitName;
    private final double unitPrice;
    private final String discountType;
    private final double discountValue;
    private final double subtotal;
    private final Integer doctorId;
    private final String doctorName;
    private final String instruction;

    PolyclinicNoteLineResponse(
        String lineType,
        Integer lineId,
        Integer referenceId,
        String code,
        String description,
        double quantity,
        String unitName,
        double unitPrice,
        String discountType,
        double discountValue,
        double subtotal,
        Integer doctorId,
        String doctorName,
        String instruction
    ) {
        this.lineType = lineType;
        this.lineId = lineId;
        this.referenceId = referenceId;
        this.code = code;
        this.description = description;
        this.quantity = quantity;
        this.unitName = unitName;
        this.unitPrice = unitPrice;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.subtotal = subtotal;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.instruction = instruction;
    }

    public String getLineType() {
        return lineType;
    }

    public Integer getLineId() {
        return lineId;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnitName() {
        return unitName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getDiscountType() {
        return discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getInstruction() {
        return instruction;
    }
}

class PolyclinicSaveResultResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final Integer statusCode;
    private final String statusLabel;
    private final String medicalRecordCode;
    private final String registrationCode;

    PolyclinicSaveResultResponse(
        Integer noteId,
        String noteNumber,
        Integer statusCode,
        String statusLabel,
        String medicalRecordCode,
        String registrationCode
    ) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.medicalRecordCode = medicalRecordCode;
        this.registrationCode = registrationCode;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getMedicalRecordCode() {
        return medicalRecordCode;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }
}

class PolyclinicActionResultResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final Integer statusCode;
    private final String statusLabel;

    PolyclinicActionResultResponse(Integer noteId, String noteNumber, Integer statusCode, String statusLabel) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
}

class PolyclinicCancelRequest {
    @NotBlank
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

class PolyclinicSaveRequest {
    @NotNull
    private Integer unitId;
    @NotNull
    private Boolean referencePatient;
    private String existingMrCode;
    private Integer patientTypeId;
    private String patientName;
    private String gender;
    private String birthDate;
    private String address;
    private Integer escortId;
    @Valid
    @NotEmpty
    private List<PolyclinicLineItemRequest> lines;

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public Boolean getReferencePatient() {
        return referencePatient;
    }

    public void setReferencePatient(Boolean referencePatient) {
        this.referencePatient = referencePatient;
    }

    public String getExistingMrCode() {
        return existingMrCode;
    }

    public void setExistingMrCode(String existingMrCode) {
        this.existingMrCode = existingMrCode;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public void setPatientTypeId(Integer patientTypeId) {
        this.patientTypeId = patientTypeId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public void setEscortId(Integer escortId) {
        this.escortId = escortId;
    }

    public List<PolyclinicLineItemRequest> getLines() {
        return lines;
    }

    public void setLines(List<PolyclinicLineItemRequest> lines) {
        this.lines = lines;
    }
}

class PolyclinicLineItemRequest {
    @NotBlank
    private String lineType;
    private Integer referenceId;
    @NotNull
    @Min(1)
    private Integer quantity;
    private Double unitPrice;
    private String discountType;
    private Double discountValue;
    private String description;
    private Integer doctorStaffId;
    private String instruction;
    private Integer contextUnitId;

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDoctorStaffId() {
        return doctorStaffId;
    }

    public void setDoctorStaffId(Integer doctorStaffId) {
        this.doctorStaffId = doctorStaffId;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Integer getContextUnitId() {
        return contextUnitId;
    }

    public void setContextUnitId(Integer contextUnitId) {
        this.contextUnitId = contextUnitId;
    }
}
