package com.vone.simrs.apotik;

import com.vone.simrs.auth.AuthenticationRequiredException;
import com.vone.simrs.auth.LegacyAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ApotikService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String DEFAULT_TARIFF_CLASS = "KELAS II";
    private static final String DISCOUNT_RP = "RP";
    private static final String DISCOUNT_PERCENT = "%";
    private static final String LINE_TYPE_ITEM = "ITEM";
    private static final String LINE_TYPE_COMPOUND = "RACIKAN";
    private static final String LINE_TYPE_MISC = "MISC";
    private static final int REG_ACTIVE = 1;
    private static final int NOTE_ACTIVE = 1;
    private static final int NOTE_VALIDATED = 2;
    private static final int NOTE_CANCELED = 0;
    private static final DateTimeFormatter NOTE_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyMM");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;
    private final double pajakObatRajal;

    public ApotikService(
            JdbcTemplate jdbcTemplate,
            LegacyAuthService legacyAuthService,
            @Value("${app.pajak.obat-rajal:0.03}") double pajakObatRajal) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
        this.pajakObatRajal = pajakObatRajal;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ===================== MASTER DATA =====================

    public ApotikMastersResponse getMasters(String username) {
        return new ApotikMastersResponse(getUnits(username), getPatientTypes(), pajakObatRajal);
    }

    private List<ApotikUnitResponse> getUnits(String username) {
        return jdbcTemplate.query(
            "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                + "from ms_unit unt "
                + "join ms_staff_in_unit stfunit on stfunit.n_unit_id = unt.n_unit_id "
                + "join ms_user usr on usr.n_staff_id = stfunit.n_staff_id "
                + "where upper(usr.v_user_name) = ? "
                + "and unt.n_whouse_id is not null "
                + "order by unt.v_unit_name",
            (resultSet, rowNum) -> new ApotikUnitResponse(
                resultSet.getInt("n_unit_id"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name"),
                getNullableInteger(resultSet, "n_whouse_id")
            ),
            normalizeUsername(username)
        );
    }

    private List<ApotikPatientTypeResponse> getPatientTypes() {
        return jdbcTemplate.query(
            "select n_patient_type_id, v_tpatient, v_tpatient_desc "
                + "from ms_patient_type order by v_tpatient",
            (resultSet, rowNum) -> new ApotikPatientTypeResponse(
                resultSet.getInt("n_patient_type_id"),
                resultSet.getString("v_tpatient"),
                resultSet.getString("v_tpatient_desc")
            )
        );
    }

    // ===================== PATIENT =====================

    public List<ApotikRegisteredPatientResponse> searchRegisteredPatients(
            String mrCode, String patientName, String address) {
        if (!hasText(mrCode) && !hasText(patientName) && !hasText(address)) {
            throw new IllegalArgumentException("Salah satu field pencarian pasien harus diisi.");
        }
        return jdbcTemplate.query(
            "select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, pat.v_patient_main_addr "
                + "from tb_medical_record mr "
                + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                + "where reg.reg_status = ? "
                + "and upper(mr.v_mr_code) like ? "
                + "and upper(pat.v_patient_name) like ? "
                + "and upper(pat.v_patient_main_addr) like ? "
                + "order by mr.v_mr_code limit 100",
            (resultSet, rowNum) -> new ApotikRegisteredPatientResponse(
                resultSet.getInt("n_mr_id"),
                resultSet.getString("v_mr_code"),
                resultSet.getString("v_patient_name"),
                resultSet.getString("v_patient_main_addr")
            ),
            REG_ACTIVE,
            like(normalizeOptionalUpper(mrCode)),
            like(normalizeOptionalUpper(patientName)),
            like(normalizeOptionalUpper(address))
        );
    }

    public ApotikPatientDetailResponse getRegisteredPatientDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("No.MR pasien harus diisi.");
        }
        try {
            return jdbcTemplate.queryForObject(
                "select mr.n_mr_id, mr.v_mr_code, mr.n_patient_id, "
                    + "reg.n_reg_id, reg.v_reg_secondary_id, "
                    + "pat.n_patient_type_id, pat.v_patient_name, "
                    + "pat.v_patient_gender, pat.d_patient_dob, pat.v_patient_main_addr, "
                    + "ptype.v_tpatient_desc as patient_type_name "
                    + "from tb_medical_record mr "
                    + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                    + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                    + "left join ms_patient_type ptype on ptype.n_patient_type_id = pat.n_patient_type_id "
                    + "where reg.reg_status = ? and mr.v_mr_code = ? "
                    + "order by reg.d_registration_date desc limit 1",
                (resultSet, rowNum) -> {
                    String regCode = resultSet.getString("v_reg_secondary_id");
                    boolean isInpatient = isInpatientRegistration(regCode);
                    String tariffClass = isInpatient
                        ? determineInpatientTariffClass(resultSet.getInt("n_reg_id"))
                        : DEFAULT_TARIFF_CLASS;
                    return new ApotikPatientDetailResponse(
                        resultSet.getInt("n_patient_id"),
                        resultSet.getInt("n_mr_id"),
                        resultSet.getString("v_mr_code"),
                        resultSet.getInt("n_reg_id"), regCode,
                        getNullableInteger(resultSet, "n_patient_type_id"),
                        resultSet.getString("patient_type_name"),
                        resultSet.getString("v_patient_name"),
                        resultSet.getString("v_patient_gender"),
                        toIsoDate(resultSet.getDate("d_patient_dob")),
                        resultSet.getString("v_patient_main_addr"),
                        isInpatient, tariffClass
                    );
                },
                REG_ACTIVE,
                normalizeMrCode(mrCode)
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Registrasi pasien aktif tidak ditemukan.");
        }
    }

    private boolean isInpatientRegistration(String regCode) {
        return regCode != null && (regCode.startsWith("I-") || regCode.startsWith("I/"));
    }

    private String determineInpatientTariffClass(Integer regId) {
        try {
            return jdbcTemplate.queryForObject(
                "select tclass.v_tclass_desc from tb_bed_occupancy boc "
                    + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                    + "join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                    + "where boc.n_reg_primary_id = ? and boc.d_check_out_time is null limit 1",
                String.class, regId
            );
        } catch (EmptyResultDataAccessException e) {
            return DEFAULT_TARIFF_CLASS;
        }
    }

    // ===================== ITEMS =====================

    public List<ApotikItemOptionResponse> searchItems(
            Integer unitId, String code, String name, String tariffClass) {
        if (unitId == null) {
            throw new IllegalArgumentException("Unit harus dipilih.");
        }
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        String effectiveTariffClass = hasText(tariffClass)
            ? tariffClass.trim().toUpperCase(Locale.ROOT) : DEFAULT_TARIFF_CLASS;

        return jdbcTemplate.query(
            "select inv.n_item_id as id, item.v_item_code as code, item.v_item_name as name, "
                + "item.n_r as r, sell.n_selling_price as harga, "
                + "meas.v_mitem_end_quantify as satuan, "
                + "sum(inv.n_item_inv_qty) as jumlah, "
                + "item.n_type as tipe "
                + "from tb_item_inventory inv "
                + "join ms_item item on item.n_item_id = inv.n_item_id "
                + "join ms_item_selling_price sell on sell.n_item_id = item.n_item_id "
                + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                + "join ms_treatment_class tclass on tclass.n_tclass_id = sell.n_tclass_id "
                + "where inv.n_whouse_id = ? "
                + "and inv.n_item_inv_qty > 0 "
                + "and tclass.v_tclass_desc = ? "
                + "and upper(item.v_item_code) like ? "
                + "and upper(item.v_item_name) like ? "
                + "group by inv.n_item_id, item.v_item_code, item.v_item_name, "
                + "item.n_r, sell.n_selling_price, meas.v_mitem_end_quantify, item.n_type "
                + "order by item.v_item_name limit 200",
            (resultSet, rowNum) -> new ApotikItemOptionResponse(
                resultSet.getInt("id"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("satuan"),
                resultSet.getDouble("harga"),
                resultSet.getDouble("jumlah"),
                resultSet.getShort("r"),
                resultSet.getShort("tipe")
            ),
            warehouseId,
            effectiveTariffClass,
            like(normalizeOptionalUpper(code)),
            like(normalizeOptionalUpper(name))
        );
    }

    private Integer findWarehouseIdByUnit(Integer unitId) {
        try {
            return jdbcTemplate.queryForObject(
                "select n_whouse_id from ms_unit where n_unit_id = ?",
                Integer.class, unitId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Unit tidak ditemukan.");
        }
    }

    // ===================== NOTES (SEARCH) =====================

    public List<ApotikNoteSummaryResponse> searchNotes(
            Integer unitId, String noteNumber, String patientName) {
        if (unitId == null) {
            throw new IllegalArgumentException("Unit harus dipilih.");
        }
        return jdbcTemplate.query(
            "select note.n_exam_id, note.v_note_no, pat.v_patient_name, "
                + "note.n_exam_status, note.d_whn_create "
                + "from tb_examination note "
                + "join ms_patient pat on pat.n_patient_id = note.n_patient_id "
                + "where note.n_unit_id = ? "
                + "and note.v_note_no like ? "
                + "and upper(pat.v_patient_name) like ? "
                + "order by note.d_whn_create desc limit 100",
            (resultSet, rowNum) -> new ApotikNoteSummaryResponse(
                resultSet.getInt("n_exam_id"),
                resultSet.getString("v_note_no"),
                resultSet.getString("v_patient_name"),
                resultSet.getInt("n_exam_status"),
                getNoteStatusLabel(resultSet.getInt("n_exam_status")),
                toIsoDateTime(resultSet.getTimestamp("d_whn_create"))
            ),
            unitId,
            like(normalizeOptionalUpper(noteNumber)),
            like(normalizeOptionalUpper(patientName))
        );
    }

    // ===================== NOTES (DETAIL) =====================

    public ApotikNoteDetailResponse getNoteDetail(Integer noteId) {
        if (noteId == null) {
            throw new IllegalArgumentException("ID nota harus diisi.");
        }
        ApotikNoteHeader header = findNoteHeader(noteId);
        List<ApotikNoteLineResponse> lines = new ArrayList<>();
        lines.addAll(getItemLines(noteId));
        lines.addAll(getCompoundLines(noteId));
        lines.addAll(getMiscLines(noteId));

        int status = header.getStatusCode();
        return new ApotikNoteDetailResponse(
            header.getNoteId(), header.getNoteNumber(), status,
            getNoteStatusLabel(status), header.getTotalAmount(),
            header.getUnitId(), header.getUnitCode(), header.getUnitName(),
            header.getPatientId(), header.getPatientTypeId(),
            header.getPatientName(), header.getGender(),
            header.getBirthDate(), header.getAddress(),
            header.getMedicalRecordCode(), header.getRegistrationId(),
            header.getRegistrationCode(), header.getReceiptNumber(),
            header.isInpatient(), header.getTariffClass(),
            header.getCancelationNote(),
            status == NOTE_ACTIVE, status == NOTE_ACTIVE,
            status == NOTE_ACTIVE || status == NOTE_VALIDATED,
            lines
        );
    }

    // ===================== NOTES (CREATE / UPDATE / VALIDATE / CANCEL) =====================

    @Transactional
    public ApotikSaveResultResponse createNote(ApotikSaveRequest request, String username) {
        UnitRow unit = findUnit(request.getUnitId());
        if (unit.getWarehouseId() == null) {
            throw new IllegalArgumentException("Unit tidak memiliki gudang (warehouse).");
        }
        RegisteredContext ctx = resolvePatientContext(request);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Integer sequence = getNextSequence("tb_examination_n_exam_id_seq");
        String noteNumber = generateNoteNumber(sequence, now, unit.getUnitCode());
        double totalAmount = calculateTotalAmount(request.getLines());

        jdbcTemplate.update(
            "insert into tb_examination (v_note_no, n_exam_status, d_whn_create, v_who_create, "
                + "n_unit_id, n_patient_id, n_reg_id, v_recipe_no, n_total_amount) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            noteNumber, NOTE_ACTIVE, now, username,
            request.getUnitId(), ctx.getPatientId(), ctx.getRegistrationId(),
            normalizeOptional(request.getReceiptNumber()), totalAmount
        );

        Integer noteId = jdbcTemplate.queryForObject(
            "select currval('tb_examination_n_exam_id_seq')", Integer.class
        );
        saveNoteLines(noteId, request.getLines(), unit.getWarehouseId(), username, now);
        return new ApotikSaveResultResponse(noteId, noteNumber);
    }

    @Transactional
    public ApotikSaveResultResponse updateNote(Integer noteId, ApotikSaveRequest request, String username) {
        ApotikNoteHeader existing = findNoteHeader(noteId);
        if (existing.getStatusCode() != NOTE_ACTIVE) {
            throw new IllegalStateException("Hanya nota aktif yang bisa diubah.");
        }
        UnitRow unit = findUnit(request.getUnitId());
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Restore inventory untuk item lama sebelum di-delete (sama seperti legacy)
        restoreNoteInventory(noteId, unit.getWarehouseId());
        deleteNoteLines(noteId);

        double totalAmount = calculateTotalAmount(request.getLines());

        jdbcTemplate.update(
            "update tb_examination set n_total_amount = ?, d_whn_change = ?, v_who_change = ?, "
                + "v_recipe_no = ?, n_unit_id = ? where n_exam_id = ?",
            totalAmount, now, username,
            normalizeOptional(request.getReceiptNumber()),
            request.getUnitId(), noteId
        );
        saveNoteLines(noteId, request.getLines(), unit.getWarehouseId(), username, now);
        return new ApotikSaveResultResponse(noteId, existing.getNoteNumber());
    }

    @Transactional
    public ApotikActionResultResponse validateNote(Integer noteId, String username) {
        ApotikNoteHeader header = findNoteHeader(noteId);
        if (header.getStatusCode() != NOTE_ACTIVE) {
            throw new IllegalStateException("Hanya nota aktif yang bisa divalidasi.");
        }
        UnitRow unit = findUnit(header.getUnitId());
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Inventory sudah di-deduct saat save (sama seperti legacy),
        // validasi hanya membuat journal entry dan mengubah status.

        jdbcTemplate.update(
            "update tb_examination set n_exam_status = ?, d_whn_change = ?, v_who_change = ? "
                + "where n_exam_id = ?",
            NOTE_VALIDATED, now, username, noteId
        );

        createApotikJournal(noteId, header, unit, now, username);

        return new ApotikActionResultResponse(true, "Nota berhasil divalidasi.");
    }

    @Transactional
    public ApotikActionResultResponse cancelNote(Integer noteId, ApotikCancelRequest request, String username) {
        ApotikNoteHeader header = findNoteHeader(noteId);
        if (header.getStatusCode() == NOTE_CANCELED) {
            throw new IllegalStateException("Nota sudah dibatalkan.");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        UnitRow unit = findUnit(header.getUnitId());

        // Restore inventory untuk semua status (ACTIVE maupun VALIDATED)
        // karena inventory sudah di-deduct saat save (sama seperti legacy)
        restoreNoteInventory(noteId, unit.getWarehouseId());

        jdbcTemplate.update(
            "update tb_examination set n_exam_status = ?, v_cancelation_note = ?, "
                + "d_whn_change = ?, v_who_change = ? where n_exam_id = ?",
            NOTE_CANCELED, request.getReason(), now, username, noteId
        );
        return new ApotikActionResultResponse(true, "Nota berhasil dibatalkan.");
    }

    // ===================== RETUR (RETURNS) =====================

    public List<ApotikReturnSummaryResponse> searchReturns(
            String returnNumber, String patientName,
            String startDate, String endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ret.n_retur_id, ret.v_retur_code, ret.n_trx_value, ")
            .append("ret.n_status, ret.d_whn_create, ")
            .append("pat.v_patient_name, nota.n_exam_id as nota_id, nota.v_note_no ")
            .append("from tb_retur_pharmacy_trx ret ")
            .append("join ms_patient pat on pat.n_patient_id = ret.n_patient_id ")
            .append("left join tb_examination nota on nota.n_exam_id = ret.n_note_id ")
            .append("where 1=1 ");
        List<Object> params = new ArrayList<>();
        if (hasText(returnNumber)) {
            sql.append("and ret.v_retur_code like ? ");
            params.add(like(returnNumber.trim().toUpperCase(Locale.ROOT)));
        }
        if (hasText(patientName)) {
            sql.append("and upper(pat.v_patient_name) like ? ");
            params.add(like(patientName.trim().toUpperCase(Locale.ROOT)));
        }
        if (hasText(startDate)) {
            sql.append("and ret.d_whn_create >= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(startDate).atStartOfDay()));
        }
        if (hasText(endDate)) {
            sql.append("and ret.d_whn_create <= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(endDate).atTime(23, 59, 59)));
        }
        sql.append("order by ret.d_whn_create desc limit 100");
        return jdbcTemplate.query(sql.toString(), params.toArray(),
            (resultSet, rowNum) -> new ApotikReturnSummaryResponse(
                resultSet.getInt("n_retur_id"),
                resultSet.getString("v_retur_code"),
                getNullableInteger(resultSet, "nota_id"),
                resultSet.getString("v_note_no"),
                resultSet.getString("v_patient_name"),
                resultSet.getDouble("n_trx_value"),
                resultSet.getInt("n_status"),
                getNoteStatusLabel(resultSet.getInt("n_status")),
                toIsoDateTime(resultSet.getTimestamp("d_whn_create"))
            )
        );
    }

    public ApotikReturnDetailResponse getReturnDetail(Integer returnId) {
        if (returnId == null) {
            throw new IllegalArgumentException("ID retur harus diisi.");
        }
        try {
            ApotikReturnHeader header = jdbcTemplate.queryForObject(
                "select ret.n_retur_id, ret.v_retur_code, ret.n_status, "
                    + "ret.n_trx_value, ret.d_whn_create, ret.v_cancelation_note, "
                    + "nota.n_exam_id as nota_id, nota.v_note_no, "
                    + "pat.v_patient_name, pat.v_patient_gender, "
                    + "pat.d_patient_dob, pat.v_patient_main_addr, "
                    + "mr.v_mr_code "
                    + "from tb_retur_pharmacy_trx ret "
                    + "join ms_patient pat on pat.n_patient_id = ret.n_patient_id "
                    + "left join tb_examination nota on nota.n_exam_id = ret.n_note_id "
                    + "left join tb_registration reg on reg.n_reg_id = ret.n_reg_id "
                    + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                    + "where ret.n_retur_id = ?",
                (resultSet, rowNum) -> new ApotikReturnHeader(
                    resultSet.getInt("n_retur_id"),
                    resultSet.getString("v_retur_code"),
                    getNullableInteger(resultSet, "nota_id"),
                    resultSet.getString("v_note_no"),
                    resultSet.getString("v_patient_name"),
                    resultSet.getString("v_patient_gender"),
                    toIsoDate(resultSet.getDate("d_patient_dob")),
                    resultSet.getString("v_patient_main_addr"),
                    resultSet.getString("v_mr_code"),
                    resultSet.getDouble("n_trx_value"),
                    resultSet.getInt("n_status"),
                    resultSet.getString("v_cancelation_note")
                ),
                returnId
            );
            List<ApotikReturnLineResponse> lines = getReturnLines(returnId);
            int status = header.getStatusCode();
            return new ApotikReturnDetailResponse(
                header.getReturnId(), header.getReturnNumber(),
                header.getOriginalNoteId(), header.getOriginalNoteNumber(),
                header.getPatientName(), header.getGender(),
                header.getBirthDate(), header.getAddress(),
                header.getMedicalRecordCode(), header.getTotalAmount(),
                status, getNoteStatusLabel(status),
                header.getCancelationNote(),
                status == NOTE_ACTIVE, status == NOTE_ACTIVE,
                status == NOTE_ACTIVE || status == NOTE_VALIDATED,
                lines
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Retur tidak ditemukan.");
        }
    }

    private List<ApotikReturnLineResponse> getReturnLines(Integer returnId) {
        return jdbcTemplate.query(
            "select det.n_detail_id, det.n_item_id, item.v_item_code, "
                + "item.v_item_name, meas.v_mitem_end_quantify, "
                + "det.n_total_qty, det.n_qty, det.n_value "
                + "from tb_retur_pharmacy_detail_trx det "
                + "join ms_item item on item.n_item_id = det.n_item_id "
                + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                + "where det.n_retur_id = ? order by det.n_detail_id",
            (resultSet, rowNum) -> new ApotikReturnLineResponse(
                resultSet.getInt("n_detail_id"), resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"), resultSet.getString("v_item_name"),
                resultSet.getString("v_mitem_end_quantify"),
                resultSet.getDouble("n_total_qty"), resultSet.getDouble("n_qty"),
                resultSet.getDouble("n_qty") == 0 ? 0
                    : resultSet.getDouble("n_value") / resultSet.getDouble("n_qty"),
                resultSet.getDouble("n_value")
            ),
            returnId
        );
    }

    @Transactional
    public ApotikActionResultResponse createReturn(
            Integer noteId, List<ApotikLineItemRequest> lines, String username) {
        ApotikNoteHeader note = findNoteHeader(noteId);
        if (note.getStatusCode() != NOTE_VALIDATED) {
            throw new IllegalStateException("Hanya nota yang sudah divalidasi yang bisa diretur.");
        }
        UnitRow unit = findUnit(note.getUnitId());
        Timestamp now = new Timestamp(System.currentTimeMillis());

        double totalValue = 0;
        for (ApotikLineItemRequest line : lines) {
            double qty = line.getQuantity() != null ? line.getQuantity() : 0;
            double price = line.getUnitPrice() != null ? line.getUnitPrice() : 0;
            totalValue += qty * price;
        }

        Integer sequence = getNextSequence("tb_examination_n_exam_id_seq");
        String returnCode = generateReturnNumber(sequence, now, unit.getUnitCode());

        jdbcTemplate.update(
            "insert into tb_retur_pharmacy_trx "
                + "(v_retur_code, n_note_id, n_patient_id, n_reg_id, "
                + "n_trx_value, n_status, d_whn_create, v_who_create) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?)",
            returnCode, noteId, note.getPatientId(), note.getRegistrationId(),
            totalValue, NOTE_ACTIVE, now, username
        );

        Integer returnId = jdbcTemplate.queryForObject(
            "select currval('tb_retur_pharmacy_trx_n_retur_id_seq')", Integer.class
        );

        for (ApotikLineItemRequest line : lines) {
            if (line.getReferenceId() == null || line.getQuantity() == null || line.getQuantity() <= 0) continue;
            jdbcTemplate.update(
                "insert into tb_retur_pharmacy_detail_trx "
                    + "(n_retur_id, n_item_id, n_total_qty, n_qty, n_value, d_whn_create, v_who_create) "
                    + "values (?, ?, ?, ?, ?, ?, ?)",
                returnId, line.getReferenceId(),
                getOriginalItemQuantity(noteId, line.getReferenceId()),
                line.getQuantity().shortValue(),
                line.getQuantity() * (line.getUnitPrice() != null ? line.getUnitPrice() : 0),
                now, username
            );
        }
        return new ApotikActionResultResponse(true, "Retur berhasil dibuat.");
    }

    @Transactional
    public ApotikActionResultResponse validateReturn(Integer returnId, String username) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<InventoryRestoreRow> items = jdbcTemplate.query(
            "select det.n_item_id, det.n_qty from tb_retur_pharmacy_detail_trx det "
                + "where det.n_retur_id = ?",
            (resultSet, rowNum) -> new InventoryRestoreRow(
                resultSet.getInt("n_item_id"), resultSet.getDouble("n_qty"), 0
            ),
            returnId
        );
        for (InventoryRestoreRow row : items) {
            jdbcTemplate.update(
                "update tb_item_inventory set n_item_inv_qty = n_item_inv_qty + ? "
                    + "where n_item_id = ? and n_whouse_id = (select unt.n_whouse_id from ms_unit unt "
                    + "  join tb_examination nota on nota.n_unit_id = unt.n_unit_id "
                    + "  join tb_retur_pharmacy_trx ret on ret.n_note_id = nota.n_exam_id "
                    + "  where ret.n_retur_id = ?)",
                row.getQuantity(), row.getItemId(), returnId
            );
        }
        jdbcTemplate.update(
            "update tb_retur_pharmacy_trx set n_status = ?, d_whn_change = ?, v_who_change = ? "
                + "where n_retur_id = ?",
            NOTE_VALIDATED, now, username, returnId
        );
        return new ApotikActionResultResponse(true, "Retur berhasil divalidasi.");
    }

    @Transactional
    public ApotikActionResultResponse cancelReturn(Integer returnId, String username) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        jdbcTemplate.update(
            "update tb_retur_pharmacy_trx set n_status = ?, d_whn_change = ?, v_who_change = ? "
                + "where n_retur_id = ?",
            NOTE_CANCELED, now, username, returnId
        );
        return new ApotikActionResultResponse(true, "Retur berhasil dibatalkan.");
    }

    // ===================== INTERNAL HELPERS =====================

    private ApotikNoteHeader findNoteHeader(Integer noteId) {
        try {
            return jdbcTemplate.queryForObject(
                "select note.n_exam_id, note.v_note_no, note.n_exam_status, note.n_total_amount, "
                    + "note.n_unit_id, unit.v_unit_code, unit.v_unit_name, "
                    + "note.n_patient_id, pat.n_patient_type_id, pat.v_patient_name, "
                    + "pat.v_patient_gender, pat.d_patient_dob, pat.v_patient_main_addr, "
                    + "note.v_cancelation_note, note.v_recipe_no, "
                    + "reg.n_reg_id, reg.v_reg_secondary_id, mr.v_mr_code "
                    + "from tb_examination note "
                    + "join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                    + "join ms_patient pat on pat.n_patient_id = note.n_patient_id "
                    + "left join tb_registration reg on reg.n_reg_id = note.n_reg_id "
                    + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                    + "where note.n_exam_id = ?",
                (resultSet, rowNum) -> {
                    String regCode = resultSet.getString("v_reg_secondary_id");
                    Integer regId = getNullableInteger(resultSet, "n_reg_id");
                    boolean isInpatient = isInpatientRegistration(regCode);
                    String tariffClass = isInpatient && regId != null
                        ? determineInpatientTariffClass(regId)
                        : DEFAULT_TARIFF_CLASS;
                    return new ApotikNoteHeader(
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
                        regId, regCode,
                        resultSet.getString("v_recipe_no"),
                        isInpatient, tariffClass,
                        resultSet.getString("v_cancelation_note")
                    );
                },
                noteId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Nota apotik tidak ditemukan.");
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
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Unit tidak ditemukan.");
        }
    }

    private RegisteredContext resolvePatientContext(ApotikSaveRequest request) {
        if (Boolean.TRUE.equals(request.getReferencePatient()) && hasText(request.getExistingMrCode())) {
            return findRegisteredContext(request.getExistingMrCode());
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update(
            "insert into ms_patient (v_patient_name, v_patient_gender, d_patient_dob, "
                + "v_patient_main_addr, d_whn_create, v_who_create) "
                + "values (?, ?, ?, ?, ?, ?)",
            normalizeOptionalUpper(request.getPatientName()),
            normalizeGender(request.getGender()),
            parseDate(request.getBirthDate()),
            normalizeOptionalUpper(request.getAddress()),
            now, "system"
        );
        Integer patientId = jdbcTemplate.queryForObject(
            "select currval('ms_patient_n_patient_id_seq')", Integer.class
        );

        jdbcTemplate.update(
            "insert into tb_medical_record (n_patient_id, d_whn_create) values (?, ?)",
            patientId, now
        );
        Integer mrId = jdbcTemplate.queryForObject(
            "select currval('tb_medical_record_n_mr_id_seq')", Integer.class
        );
        Integer mrNumber = getNextSequence("tb_medical_record_n_mr_id_seq");
        String mrCode = "MR-" + String.format("%06d", mrNumber);
        jdbcTemplate.update(
            "update tb_medical_record set v_mr_code = ? where n_mr_id = ?",
            mrCode, mrId
        );

        Integer regNumber = getNextSequence("apotik_reg_seq");
        String regCode = "J-APTK-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%03d", regNumber);
        jdbcTemplate.update(
            "insert into tb_registration (n_mr_id, n_patient_id, v_reg_secondary_id, "
                + "n_patient_type_id, d_registration_date, reg_status, v_who_create) "
                + "values (?, ?, ?, ?, ?, ?, ?)",
            mrId, patientId, regCode,
            request.getPatientTypeId(), now, REG_ACTIVE, "system"
        );
        Integer regId = jdbcTemplate.queryForObject(
            "select currval('tb_registration_n_reg_id_seq')", Integer.class
        );
        return new RegisteredContext(mrId, mrCode, patientId, regId, regCode);
    }

    private RegisteredContext findRegisteredContext(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                "select mr.n_mr_id, mr.v_mr_code, mr.n_patient_id, "
                    + "reg.n_reg_id, reg.v_reg_secondary_id "
                    + "from tb_medical_record mr "
                    + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                    + "where reg.reg_status = ? and mr.v_mr_code = ? "
                    + "order by reg.d_registration_date desc limit 1",
                (resultSet, rowNum) -> new RegisteredContext(
                    resultSet.getInt("n_mr_id"),
                    resultSet.getString("v_mr_code"),
                    resultSet.getInt("n_patient_id"),
                    resultSet.getInt("n_reg_id"),
                    resultSet.getString("v_reg_secondary_id")
                ),
                REG_ACTIVE,
                normalizeMrCode(mrCode)
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Registrasi pasien aktif tidak ditemukan.");
        }
    }

    private void saveNoteLines(Integer noteId, List<ApotikLineItemRequest> lines,
                                Integer warehouseId, String username, Timestamp now) {
        for (ApotikLineItemRequest line : lines) {
            if (LINE_TYPE_ITEM.equals(line.getLineType())) {
                saveItemLine(noteId, line, warehouseId, username, now);
            } else if (LINE_TYPE_COMPOUND.equals(line.getLineType())) {
                saveCompoundLine(noteId, line, warehouseId, username, now);
            } else if (LINE_TYPE_MISC.equals(line.getLineType())) {
                saveMiscLine(noteId, line, username, now);
            }
        }
    }

    private void saveItemLine(Integer noteId, ApotikLineItemRequest line,
                              Integer warehouseId, String username, Timestamp now) {
        double qty = line.getQuantity() != null ? line.getQuantity() : 0;
        double price = line.getUnitPrice() != null ? line.getUnitPrice() : 0;
        double amountBeforeDisc = qty * price;
        double discAmount = calculateDiscount(amountBeforeDisc, line.getDiscountType(), line.getDiscountValue());
        double amountAfterDisc = amountBeforeDisc - discAmount;
        String discType = normalizeDiscountType(line.getDiscountType());

        // Sama persis dengan legacy: loop semua batch yang punya stok
        double remaining = qty;
        double perUnitHarga = qty == 0 ? 0 : amountBeforeDisc / qty;
        double perUnitDisc = qty == 0 ? 0 : discAmount / qty;
        double perUnitAfterDisc = qty == 0 ? 0 : amountAfterDisc / qty;

        List<InventoryBatchRow> inventories = findItemInventories(warehouseId, line.getReferenceId());
        for (InventoryBatchRow inv : inventories) {
            if (remaining <= 0) break;

            double picked = Math.min(remaining, inv.getQuantity());

            jdbcTemplate.update(
                "insert into tb_item_trx (n_note_id, n_item_id, n_batch_id, n_qty, "
                    + "n_amount_trx, n_disc_amount, v_disc_type, n_amount_after_disc, "
                    + "aturan_pakai, d_whn_create, v_who_create) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                noteId, line.getReferenceId(), inv.getBatchId(), picked,
                picked * perUnitHarga, picked * perUnitDisc, discType, picked * perUnitAfterDisc,
                normalizeOptional(line.getInstruction()), now, username
            );

            // Deduct inventory per batch (sama persis seperti legacy)
            deductInventory(line.getReferenceId(), inv.getBatchId(), warehouseId, picked);
            remaining -= picked;
        }

        if (remaining > 0) {
            throw new IllegalStateException(
                "Stok item id " + line.getReferenceId() + " tidak mencukupi."
            );
        }
    }

    /**
     * Deduct inventory untuk item tertentu di batch & warehouse.
     */
    private void deductInventory(Integer itemId, Integer batchId, Integer warehouseId, double qty) {
        jdbcTemplate.update(
            "update tb_item_inventory set n_item_inv_qty = n_item_inv_qty - ? "
                + "where n_item_id = ? and n_batch_id = ? and n_whouse_id = ?",
            qty, itemId, batchId, warehouseId
        );
    }

    /**
     * Restore inventory untuk item tertentu (undo deduct).
     */
    private void restoreInventory(Integer itemId, Integer batchId, Integer warehouseId, double qty) {
        jdbcTemplate.update(
            "update tb_item_inventory set n_item_inv_qty = n_item_inv_qty + ? "
                + "where n_item_id = ? and n_batch_id = ? and n_whouse_id = ?",
            qty, itemId, batchId, warehouseId
        );
    }

    /**
     * Restore inventory untuk semua item trx dan compound ingredients pada suatu nota.
     */
    private void restoreNoteInventory(Integer noteId, Integer warehouseId) {
        // Restore item trx
        List<InventoryBatchRow> itemRows = jdbcTemplate.query(
            "select n_item_id, n_batch_id, n_qty from tb_item_trx where n_note_id = ?",
            (resultSet, rowNum) -> new InventoryBatchRow(
                resultSet.getInt("n_item_id"),
                resultSet.getInt("n_batch_id"),
                resultSet.getDouble("n_qty")
            ),
            noteId
        );
        for (InventoryBatchRow row : itemRows) {
            restoreInventory(row.getItemId(), row.getBatchId(), warehouseId, row.getQuantity());
        }

        // Restore compound ingredients
        List<InventoryBatchRow> compoundRows = jdbcTemplate.query(
            "select det.n_item_id, det.n_batch_id, sum(det.n_dingr_det_qty * ingr.n_dingr_qty) as total_qty "
                + "from tb_drug_ingredients_detail det "
                + "join tb_drug_ingredients ingr on ingr.n_dingr_id = det.n_dingr_id "
                + "where ingr.n_note_id = ? "
                + "group by det.n_item_id, det.n_batch_id",
            (resultSet, rowNum) -> new InventoryBatchRow(
                resultSet.getInt("n_item_id"),
                resultSet.getInt("n_batch_id"),
                resultSet.getDouble("total_qty")
            ),
            noteId
        );
        for (InventoryBatchRow row : compoundRows) {
            restoreInventory(row.getItemId(), row.getBatchId(), warehouseId, row.getQuantity());
        }
    }

    /**
     * Cari semua inventory batch untuk item tertentu di warehouse.
     * Sama persis seperti legacy yang loop semua batch yang punya stok.
     */
    private List<InventoryBatchRow> findItemInventories(Integer warehouseId, Integer itemId) {
        return jdbcTemplate.query(
            "select n_batch_id, n_item_inv_qty from tb_item_inventory "
                + "where n_whouse_id = ? and n_item_id = ? and n_item_inv_qty > 0 "
                + "order by n_batch_id",
            (resultSet, rowNum) -> new InventoryBatchRow(
                itemId,
                resultSet.getInt("n_batch_id"),
                resultSet.getDouble("n_item_inv_qty")
            ),
            warehouseId, itemId
        );
    }

    private void saveCompoundLine(Integer noteId, ApotikLineItemRequest line,
                                  Integer warehouseId, String username, Timestamp now) {
        double qty = line.getQuantity() != null ? line.getQuantity() : 0;
        double price = line.getUnitPrice() != null ? line.getUnitPrice() : 0;
        double amountBeforeDisc = qty * price;
        double discAmount = calculateDiscount(amountBeforeDisc, line.getDiscountType(), line.getDiscountValue());
        double amountAfterDisc = amountBeforeDisc - discAmount;
        String discType = normalizeDiscountType(line.getDiscountType());
        String quantify = hasText(line.getUnitName()) ? line.getUnitName() : "BUNGKUS";

        Integer compoundId = getNextSequence("tb_drug_ingredients_n_dingr_id_seq");
        String compoundCode = now.toLocalDateTime().format(DateTimeFormatter.ofPattern("ddMMyy"))
            + "-" + compoundId;

        jdbcTemplate.update(
            "insert into tb_drug_ingredients (n_dingr_id, n_note_id, v_dingr_id, "
                + "v_item_composition, n_dingr_qty, n_dingr_quantify, "
                + "n_amount_trx, n_disc_amount, v_disc_type, n_amount_after_disc, "
                + "aturan_pakai, d_whn_create, v_who_create) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            compoundId, noteId, compoundCode,
            normalizeOptional(line.getDescription()), qty, quantify,
            amountBeforeDisc, discAmount, discType, amountAfterDisc,
            normalizeOptional(line.getInstruction()), now, username
        );

        if (line.getComponents() != null) {
            for (ApotikCompoundComponentRequest comp : line.getComponents()) {
                if (comp.getReferenceId() == null || comp.getQuantity() == null) continue;

                // Sama persis dengan legacy: loop semua batch untuk tiap komponen racikan
                double remainingComp = comp.getQuantity();
                List<InventoryBatchRow> inventories = findItemInventories(warehouseId, comp.getReferenceId());

                for (InventoryBatchRow inv : inventories) {
                    if (remainingComp <= 0) break;

                    double picked = Math.min(remainingComp, inv.getQuantity());

                    jdbcTemplate.update(
                        "insert into tb_drug_ingredients_detail (n_dingr_id, n_item_id, n_batch_id, "
                            + "n_dingr_det_qty, d_whn_create, v_who_create) values (?, ?, ?, ?, ?, ?)",
                        compoundId, comp.getReferenceId(), inv.getBatchId(), picked, now, username
                    );

                    // Deduct inventory per batch (sama persis seperti legacy)
                    deductInventory(comp.getReferenceId(), inv.getBatchId(), warehouseId, picked);
                    remainingComp -= picked;
                }

                if (remainingComp > 0) {
                    throw new IllegalStateException(
                        "Stok komponen item id " + comp.getReferenceId() + " tidak mencukupi untuk racikan."
                    );
                }
            }
        }
    }

    private void saveMiscLine(Integer noteId, ApotikLineItemRequest line, String username, Timestamp now) {
        double qty = line.getQuantity() != null ? line.getQuantity() : 0;
        double price = line.getUnitPrice() != null ? line.getUnitPrice() : 0;
        double amountBeforeDisc = qty * price;
        double discAmount = calculateDiscount(amountBeforeDisc, line.getDiscountType(), line.getDiscountValue());
        double amountAfterDisc = amountBeforeDisc - discAmount;
        String discType = normalizeDiscountType(line.getDiscountType());
        jdbcTemplate.update(
            "insert into tb_misc_trx (n_note_id, v_misc_name, n_qty, n_item_price, "
                + "n_amount_trx, n_disc_amount, v_disc_type, n_amount_after_disc, "
                + "d_whn_create, v_who_create) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            noteId, normalizeOptional(line.getDescription()), (short) qty,
            price, amountBeforeDisc, discAmount, discType, amountAfterDisc,
            now, username
        );
    }

    private void deleteNoteLines(Integer noteId) {
        jdbcTemplate.update("delete from tb_item_trx where n_note_id = ?", noteId);
        jdbcTemplate.update("delete from tb_drug_ingredients_detail where n_dingr_id in "
            + "(select n_dingr_id from tb_drug_ingredients where n_note_id = ?)", noteId);
        jdbcTemplate.update("delete from tb_drug_ingredients where n_note_id = ?", noteId);
        jdbcTemplate.update("delete from tb_misc_trx where n_note_id = ?", noteId);
    }

    private List<ApotikNoteLineResponse> getItemLines(Integer noteId) {
        return jdbcTemplate.query(
            "select trx.n_item_trx_id, item.v_item_code, item.v_item_name, "
                + "meas.v_mitem_end_quantify, trx.n_qty, "
                + "trx.n_amount_trx, trx.n_disc_amount, trx.v_disc_type, "
                + "trx.n_amount_after_disc, trx.aturan_pakai "
                + "from tb_item_trx trx "
                + "join ms_item item on item.n_item_id = trx.n_item_id "
                + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                + "where trx.n_note_id = ? order by trx.n_item_trx_id",
            (resultSet, rowNum) -> {
                double qty = resultSet.getDouble("n_qty");
                double amount = resultSet.getDouble("n_amount_trx");
                return new ApotikNoteLineResponse(
                    LINE_TYPE_ITEM,
                    resultSet.getInt("n_item_trx_id"),
                    resultSet.getInt("n_item_trx_id"),
                    resultSet.getString("v_item_code"),
                    resultSet.getString("v_item_name"),
                    qty,
                    resultSet.getString("v_mitem_end_quantify"),
                    qty == 0 ? 0 : amount / qty,
                    normalizeDiscountType(resultSet.getString("v_disc_type")),
                    resultSet.getDouble("n_disc_amount"),
                    resultSet.getDouble("n_amount_after_disc"),
                    resultSet.getString("aturan_pakai"),
                    null
                );
            },
            noteId
        );
    }

    private List<ApotikNoteLineResponse> getCompoundLines(Integer noteId) {
        return jdbcTemplate.query(
            "select n_dingr_id, v_dingr_id, v_item_composition, n_dingr_qty, "
                + "n_dingr_quantify, n_amount_trx, n_disc_amount, v_disc_type, "
                + "n_amount_after_disc, aturan_pakai "
                + "from tb_drug_ingredients where n_note_id = ? order by n_dingr_id",
            (resultSet, rowNum) -> {
                double qty = resultSet.getDouble("n_dingr_qty");
                double amount = resultSet.getDouble("n_amount_trx");
                Integer compoundId = resultSet.getInt("n_dingr_id");
                return new ApotikNoteLineResponse(
                    LINE_TYPE_COMPOUND,
                    compoundId, compoundId,
                    resultSet.getString("v_dingr_id"),
                    resultSet.getString("v_item_composition"),
                    qty,
                    resultSet.getString("n_dingr_quantify"),
                    qty == 0 ? 0 : amount / qty,
                    normalizeDiscountType(resultSet.getString("v_disc_type")),
                    resultSet.getDouble("n_disc_amount"),
                    resultSet.getDouble("n_amount_after_disc"),
                    resultSet.getString("aturan_pakai"),
                    getCompoundComponents(compoundId, qty)
                );
            },
            noteId
        );
    }

    private List<ApotikNoteLineResponse> getMiscLines(Integer noteId) {
        return jdbcTemplate.query(
            "select n_misc_trx_id, v_misc_name, n_qty, n_item_price, "
                + "n_disc_amount, v_disc_type, n_amount_after_disc "
                + "from tb_misc_trx where n_note_id = ? order by n_misc_trx_id",
            (resultSet, rowNum) -> new ApotikNoteLineResponse(
                LINE_TYPE_MISC,
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
                null, null
            ),
            noteId
        );
    }

    private List<ApotikCompoundComponentResponse> getCompoundComponents(Integer compoundId, double compoundQty) {
        return jdbcTemplate.query(
            "select det.n_item_id, item.v_item_code, item.v_item_name, "
                + "meas.v_mitem_end_quantify, sum(det.n_dingr_det_qty) as qty "
                + "from tb_drug_ingredients_detail det "
                + "join ms_item item on item.n_item_id = det.n_item_id "
                + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                + "where det.n_dingr_id = ? "
                + "group by det.n_item_id, item.v_item_code, item.v_item_name, "
                + "meas.v_mitem_end_quantify order by item.v_item_name",
            (resultSet, rowNum) -> new ApotikCompoundComponentResponse(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_mitem_end_quantify"),
                compoundQty <= 0 ? 0 : resultSet.getDouble("qty") / compoundQty
            ),
            compoundId
        );
    }

    private double getOriginalItemQuantity(Integer noteId, Integer itemId) {
        try {
            Double qty = jdbcTemplate.queryForObject(
                "select sum(n_qty) from tb_item_trx where n_note_id = ? and n_item_id = ?",
                Double.class, noteId, itemId
            );
            return qty != null ? qty : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private double calculateTotalAmount(List<ApotikLineItemRequest> lines) {
        double total = 0;
        for (ApotikLineItemRequest line : lines) {
            double qty = line.getQuantity() != null ? line.getQuantity() : 0;
            double price = line.getUnitPrice() != null ? line.getUnitPrice() : 0;
            double amountBeforeDisc = qty * price;
            double discAmount = calculateDiscount(amountBeforeDisc, line.getDiscountType(), line.getDiscountValue());
            total += amountBeforeDisc - discAmount;
        }
        return total;
    }

    private double calculateDiscount(double amount, String discType, Double discValue) {
        if (discValue == null || discValue <= 0) return 0;
        if (DISCOUNT_RP.equals(discType)) return discValue;
        if (DISCOUNT_PERCENT.equals(discType)) return amount * discValue / 100.0;
        return 0;
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject(
            "select nextval('" + sequenceName + "')", Integer.class
        );
    }

    private String generateNoteNumber(Integer sequence, Timestamp date, String unitCode) {
        return "J-APTK-" + date.toLocalDateTime().format(NOTE_NUMBER_FORMAT)
            + "-" + String.format("%06d", sequence);
    }

    private String generateReturnNumber(Integer sequence, Timestamp date, String unitCode) {
        return "R-APTK-" + date.toLocalDateTime().format(NOTE_NUMBER_FORMAT)
            + "-" + String.format("%06d", sequence);
    }

    // ===================== UTILITY METHODS =====================

    private String getNoteStatusLabel(int status) {
        switch (status) {
            case 0: return "BATAL";
            case 1: return "AKTIF";
            case 2: return "VALIDASI";
            case 3: return "LUNAS";
            case 4: return "VALIDASI BATAL";
            default: return "UNKNOWN";
        }
    }

    private String normalizeDiscountType(String discType) {
        if (DISCOUNT_PERCENT.equals(discType)) return DISCOUNT_PERCENT;
        return DISCOUNT_RP;
    }

    private String normalizeMrCode(String mrCode) {
        return mrCode == null ? "" : mrCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalizeOptionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeGender(String value) {
        return "F".equalsIgnoreCase(value) ? "F" : "M";
    }

    private String toIsoDate(Date date) {
        return date == null ? "" : date.toLocalDate().toString();
    }

    private String toIsoDateTime(Timestamp ts) {
        return ts == null ? "" : ts.toLocalDateTime().toString();
    }

    private Date parseDate(String dateStr) {
        if (!hasText(dateStr)) return null;
        try {
            return Date.valueOf(LocalDate.parse(dateStr, ISO_DATE));
        } catch (Exception e) {
            return null;
        }
    }

    private String like(String value) {
        return "%" + (value != null ? value : "") + "%";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName) throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    // ===================== ACCOUNTING JOURNAL =====================

    private void createApotikJournal(Integer noteId, ApotikNoteHeader header,
                                     UnitRow unit, Timestamp now, String username) {
        String batchId = buildJournalBatchId();
        String voucherNo = header.getNoteNumber();
        boolean ip = header.isInpatient();
        String arCoaKey = ip ? "COA_INPATIENT_AR" : "COA_OUTPATIENT_AR";
        Integer coaArId = findCoaIdByGimKey(arCoaKey);
        if (coaArId == null) throw new IllegalStateException(
            "COA AR untuk " + (ip ? "rawat inap" : "rawat jalan") + " belum dikonfigurasi.");
        Integer coaInvId = findWarehouseCoaId(unit.getWarehouseId());
        if (coaInvId == null) throw new IllegalStateException(
            "COA inventory warehouse #" + unit.getWarehouseId() + " belum dikonfigurasi.");
        // ITEM lines
        for (JournalItemLine line : getItemLinesForJournal(noteId)) {
            Integer coaIncomeId = findItemSellCoaId(line.itemId);
            Integer coaCogsId = findItemCogsCoaId(line.itemId);
            if (coaIncomeId == null || coaCogsId == null)
                throw new IllegalStateException("COA penjualan/COGS item #" + line.itemId + " belum dikonfigurasi.");
            double cogs = calculateItemCogs(line.itemId, line.qty);
            String m = "ITEMCODE:" + line.itemCode + ";QTY:" + line.qty + ";DISCOUNT:" + line.discAmount;
            insertJournal(batchId, voucherNo, m, line.amountAfterDisc, 0, now, username, coaArId);
            insertJournal(batchId, voucherNo, m, 0, line.amountAfterDisc, now, username, coaIncomeId);
            insertJournal(batchId, voucherNo, m, 0, cogs, now, username, coaInvId);
            insertJournal(batchId, voucherNo, m, cogs, 0, now, username, coaCogsId);
        }
        // COMPOUND lines
        for (JournalCompoundLine cpd : getCompoundLinesForJournal(noteId)) {
            double totalCogs = 0;
            for (JournalCompoundComponent comp : cpd.components) totalCogs += calculateItemCogs(comp.itemId, comp.qty);
            String m = "ITEMCODE:" + cpd.compoundCode + ";QTY:" + cpd.qty + ";DISCOUNT:" + cpd.discAmount;
            insertJournal(batchId, voucherNo, m, cpd.amountAfterDisc, 0, now, username, coaArId);
            insertJournal(batchId, voucherNo, m, 0, cpd.amountAfterDisc, now, username, coaInvId);
            insertJournal(batchId, voucherNo, m, 0, totalCogs, now, username, coaInvId);
            insertJournal(batchId, voucherNo, m, totalCogs, 0, now, username, coaInvId);
        }
        // MISC lines
        Integer coaMiscId = findCoaIdByGimKey("COA_MISC_TRX");
        if (coaMiscId == null) throw new IllegalStateException("COA misc belum dikonfigurasi.");
        for (JournalMiscLine misc : getMiscLinesForJournal(noteId)) {
            String m = "MISC:" + misc.miscName + ";QTY:" + misc.qty;
            insertJournal(batchId, voucherNo, m, misc.amountAfterDisc, 0, now, username, coaArId);
            insertJournal(batchId, voucherNo, m, 0, misc.amountAfterDisc, now, username, coaMiscId);
        }
    }

    // ===================== JOURNAL HELPERS =====================

    private void insertJournal(String batchId, String voucherNo, String desc,
            double debit, double credit, Timestamp now, String username, Integer coaId) {
        Integer journalId = getNextSequence("tb_journal_trx_n_journal_id_seq");
        jdbcTemplate.update(
            "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, "
                + "v_desc, n_debit, n_credit, d_whn_create, v_who_create, d_apl_date, n_coa_id) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            journalId, batchId, voucherNo, desc, debit, credit, now, username, now, coaId);
    }

    private Integer findCoaIdByGimKey(String gimKey) {
        try { return jdbcTemplate.queryForObject(
            "select coa.n_coa_id from ms_gim gim join ms_coa coa on coa.v_acct_no = gim.v_value where gim.v_key = ?",
            Integer.class, gimKey);
        } catch (EmptyResultDataAccessException e) { return null; }
    }

    private Integer findWarehouseCoaId(Integer wid) {
        try { return jdbcTemplate.queryForObject("select n_coa_id from ms_warehouse where n_whouse_id = ?", Integer.class, wid);
        } catch (EmptyResultDataAccessException e) { return null; }
    }

    private Integer findItemSellCoaId(Integer itemId) {
        try { return jdbcTemplate.queryForObject("select n_item_sell_acc_no from ms_item where n_item_id = ?", Integer.class, itemId);
        } catch (EmptyResultDataAccessException e) { return null; }
    }

    private Integer findItemCogsCoaId(Integer itemId) {
        try { return jdbcTemplate.queryForObject("select n_item_cogs_no from ms_item where n_item_id = ?", Integer.class, itemId);
        } catch (EmptyResultDataAccessException e) { return null; }
    }

    private double calculateItemCogs(Integer itemId, double qty) {
        try {
            Double avg = jdbcTemplate.queryForObject(
                "select coalesce(avg(batch.n_cogs_price), 0) from tb_batch_item batch "
                    + "join tb_item_inventory inv on inv.n_batch_id = batch.n_batch_id "
                    + "where inv.n_item_id = ? and inv.n_item_inv_qty > 0", Double.class, itemId);
            return (avg != null ? avg : 0) * qty;
        } catch (EmptyResultDataAccessException e) { return 0; }
    }

    private String buildJournalBatchId() {
        return "AR" + String.format("%015d", getNextSequence("sq_journal_trx"));
    }

    // ===================== JOURNAL QUERIES =====================

    private List<JournalItemLine> getItemLinesForJournal(Integer noteId) {
        return jdbcTemplate.query(
            "select trx.n_item_id, item.v_item_code, trx.n_qty, "
                + "trx.n_amount_trx, trx.n_disc_amount, trx.n_amount_after_disc "
                + "from tb_item_trx trx join ms_item item on item.n_item_id = trx.n_item_id where trx.n_note_id = ?",
            (rs, rn) -> new JournalItemLine(rs.getInt("n_item_id"), rs.getString("v_item_code"),
                rs.getDouble("n_qty"), rs.getDouble("n_amount_trx"),
                rs.getDouble("n_disc_amount"), rs.getDouble("n_amount_after_disc")), noteId);
    }

    private List<JournalCompoundLine> getCompoundLinesForJournal(Integer noteId) {
        return jdbcTemplate.query(
            "select ingr.n_dingr_id, ingr.v_dingr_id, ingr.n_dingr_qty, "
                + "ingr.n_amount_trx, ingr.n_disc_amount, ingr.n_amount_after_disc "
                + "from tb_drug_ingredients ingr where ingr.n_note_id = ?",
            (rs, rn) -> {
                Integer cid = rs.getInt("n_dingr_id");
                List<JournalCompoundComponent> comps = getCompoundComponentsForJournal(cid);
                return new JournalCompoundLine(cid, rs.getString("v_dingr_id"),
                    rs.getDouble("n_dingr_qty"), rs.getDouble("n_amount_trx"),
                    rs.getDouble("n_disc_amount"), rs.getDouble("n_amount_after_disc"), comps);
            }, noteId);
    }

    private List<JournalCompoundComponent> getCompoundComponentsForJournal(Integer compoundId) {
        return jdbcTemplate.query(
            "select det.n_item_id, sum(det.n_dingr_det_qty) as qty "
                + "from tb_drug_ingredients_detail det where det.n_dingr_id = ? group by det.n_item_id",
            (rs, rn) -> new JournalCompoundComponent(rs.getInt("n_item_id"), rs.getDouble("qty")), compoundId);
    }

    private List<JournalMiscLine> getMiscLinesForJournal(Integer noteId) {
        return jdbcTemplate.query(
            "select v_misc_name, n_qty, n_amount_trx, n_disc_amount, n_amount_after_disc "
                + "from tb_misc_trx where n_note_id = ?",
            (rs, rn) -> new JournalMiscLine(rs.getString("v_misc_name"), rs.getDouble("n_qty"),
                rs.getDouble("n_amount_trx"), rs.getDouble("n_disc_amount"), rs.getDouble("n_amount_after_disc")), noteId);
    }

    // ===================== JOURNAL INNER CLASSES =====================

    private static class JournalItemLine {
        final int itemId; final String itemCode; final double qty;
        final double amountTrx; final double discAmount; final double amountAfterDisc;
        JournalItemLine(int itemId, String itemCode, double qty,
                        double amountTrx, double discAmount, double amountAfterDisc) {
            this.itemId = itemId; this.itemCode = itemCode; this.qty = qty;
            this.amountTrx = amountTrx; this.discAmount = discAmount; this.amountAfterDisc = amountAfterDisc;
        }
    }

    private static class JournalCompoundLine {
        final int compoundId; final String compoundCode; final double qty;
        final double amountTrx; final double discAmount; final double amountAfterDisc;
        final List<JournalCompoundComponent> components;
        JournalCompoundLine(int compoundId, String compoundCode, double qty,
                            double amountTrx, double discAmount, double amountAfterDisc,
                            List<JournalCompoundComponent> components) {
            this.compoundId = compoundId; this.compoundCode = compoundCode; this.qty = qty;
            this.amountTrx = amountTrx; this.discAmount = discAmount; this.amountAfterDisc = amountAfterDisc;
            this.components = components;
        }
    }

    private static class JournalCompoundComponent {
        final int itemId; final double qty;
        JournalCompoundComponent(int itemId, double qty) {
            this.itemId = itemId; this.qty = qty;
        }
    }

    private static class JournalMiscLine {
        final String miscName; final double qty; final double amountTrx;
        final double discAmount; final double amountAfterDisc;
        JournalMiscLine(String miscName, double qty, double amountTrx,
                        double discAmount, double amountAfterDisc) {
            this.miscName = miscName; this.qty = qty; this.amountTrx = amountTrx;
            this.discAmount = discAmount; this.amountAfterDisc = amountAfterDisc;
        }
    }

    // ===================== INNER CLASSES =====================

    private static class ApotikNoteHeader {
        private final Integer noteId; private final String noteNumber;
        private final Integer statusCode; private final double totalAmount;
        private final Integer unitId; private final String unitCode;
        private final String unitName; private final Integer patientId;
        private final Integer patientTypeId; private final String patientName;
        private final String gender; private final String birthDate;
        private final String address; private final String mrCode;
        private final Integer registrationId; private final String registrationCode;
        private final String receiptNumber; private final boolean inpatient;
        private final String tariffClass; private final String cancelationNote;

        ApotikNoteHeader(Integer noteId, String noteNumber, Integer statusCode,
                double totalAmount, Integer unitId, String unitCode, String unitName,
                Integer patientId, Integer patientTypeId, String patientName,
                String gender, String birthDate, String address, String mrCode,
                Integer registrationId, String registrationCode, String receiptNumber,
                boolean inpatient, String tariffClass, String cancelationNote) {
            this.noteId = noteId; this.noteNumber = noteNumber;
            this.statusCode = statusCode; this.totalAmount = totalAmount;
            this.unitId = unitId; this.unitCode = unitCode; this.unitName = unitName;
            this.patientId = patientId; this.patientTypeId = patientTypeId;
            this.patientName = patientName; this.gender = gender;
            this.birthDate = birthDate; this.address = address; this.mrCode = mrCode;
            this.registrationId = registrationId; this.registrationCode = registrationCode;
            this.receiptNumber = receiptNumber; this.inpatient = inpatient;
            this.tariffClass = tariffClass; this.cancelationNote = cancelationNote;
        }
        Integer getNoteId() { return noteId; }
        String getNoteNumber() { return noteNumber; }
        Integer getStatusCode() { return statusCode; }
        double getTotalAmount() { return totalAmount; }
        Integer getUnitId() { return unitId; }
        String getUnitCode() { return unitCode; }
        String getUnitName() { return unitName; }
        Integer getPatientId() { return patientId; }
        Integer getPatientTypeId() { return patientTypeId; }
        String getPatientName() { return patientName; }
        String getGender() { return gender; }
        String getBirthDate() { return birthDate; }
        String getAddress() { return address; }
        String getMedicalRecordCode() { return mrCode; }
        Integer getRegistrationId() { return registrationId; }
        String getRegistrationCode() { return registrationCode; }
        String getReceiptNumber() { return receiptNumber; }
        boolean isInpatient() { return inpatient; }
        String getTariffClass() { return tariffClass; }
        String getCancelationNote() { return cancelationNote; }
    }

    private static class ApotikReturnHeader {
        private final Integer returnId; private final String returnNumber;
        private final Integer originalNoteId; private final String originalNoteNumber;
        private final String patientName; private final String gender;
        private final String birthDate; private final String address;
        private final String mrCode; private final double totalAmount;
        private final Integer statusCode; private final String cancelationNote;

        ApotikReturnHeader(Integer returnId, String returnNumber,
                Integer originalNoteId, String originalNoteNumber,
                String patientName, String gender, String birthDate,
                String address, String mrCode, double totalAmount,
                Integer statusCode, String cancelationNote) {
            this.returnId = returnId; this.returnNumber = returnNumber;
            this.originalNoteId = originalNoteId;
            this.originalNoteNumber = originalNoteNumber;
            this.patientName = patientName; this.gender = gender;
            this.birthDate = birthDate; this.address = address;
            this.mrCode = mrCode; this.totalAmount = totalAmount;
            this.statusCode = statusCode; this.cancelationNote = cancelationNote;
        }
        Integer getReturnId() { return returnId; }
        String getReturnNumber() { return returnNumber; }
        Integer getOriginalNoteId() { return originalNoteId; }
        String getOriginalNoteNumber() { return originalNoteNumber; }
        String getPatientName() { return patientName; }
        String getGender() { return gender; }
        String getBirthDate() { return birthDate; }
        String getAddress() { return address; }
        String getMedicalRecordCode() { return mrCode; }
        double getTotalAmount() { return totalAmount; }
        Integer getStatusCode() { return statusCode; }
        String getCancelationNote() { return cancelationNote; }
    }

    private static class UnitRow {
        private final Integer unitId; private final String unitCode;
        private final String unitName; private final Integer warehouseId;
        UnitRow(Integer unitId, String unitCode, String unitName, Integer warehouseId) {
            this.unitId = unitId; this.unitCode = unitCode;
            this.unitName = unitName; this.warehouseId = warehouseId;
        }
        Integer getUnitId() { return unitId; }
        String getUnitCode() { return unitCode; }
        String getUnitName() { return unitName; }
        Integer getWarehouseId() { return warehouseId; }
    }

    private static class RegisteredContext {
        private final Integer medicalRecordId; private final String mrCode;
        private final Integer patientId; private final Integer registrationId;
        private final String registrationCode;
        RegisteredContext(Integer medicalRecordId, String mrCode,
                Integer patientId, Integer registrationId, String registrationCode) {
            this.medicalRecordId = medicalRecordId; this.mrCode = mrCode;
            this.patientId = patientId; this.registrationId = registrationId;
            this.registrationCode = registrationCode;
        }
        Integer getMedicalRecordId() { return medicalRecordId; }
        String getMrCode() { return mrCode; }
        Integer getPatientId() { return patientId; }
        Integer getRegistrationId() { return registrationId; }
        String getRegistrationCode() { return registrationCode; }
    }

    private static class InventoryRestoreRow {
        private final Integer itemId; private final double quantity;
        private final double currentStock;
        InventoryRestoreRow(Integer itemId, double quantity, double currentStock) {
            this.itemId = itemId; this.quantity = quantity; this.currentStock = currentStock;
        }
        Integer getItemId() { return itemId; }
        double getQuantity() { return quantity; }
        double getCurrentStock() { return currentStock; }
    }

    /**
     * Inventory batch row used for batch-aware inventory operations.
     * Mirrors the approach used in PolyclinicService for handling batch_id.
     */
    private static class InventoryBatchRow {
        private final Integer itemId;
        private final Integer batchId;
        private final double quantity;

        InventoryBatchRow(Integer itemId, Integer batchId, double quantity) {
            this.itemId = itemId;
            this.batchId = batchId;
            this.quantity = quantity;
        }

        Integer getItemId() { return itemId; }
        Integer getBatchId() { return batchId; }
        double getQuantity() { return quantity; }
    }
}
