package com.vone.simrs.apotik;

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
import javax.validation.constraints.DecimalMin;
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
    private static final int NOTE_VALIDATED_CANCELED = 4;
    private static final short PAYMENT_UNPAID = 0;
    private static final SimpleDateFormat NOTE_DATE_FORMAT = new SimpleDateFormat("yyMM");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public ApotikService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    public ApotikMastersResponse getMasters(String username) {
        return new ApotikMastersResponse(getUnits(username), getPatientTypes());
    }

    public List<ApotikRegisteredPatientResponse> searchRegisteredPatients(String mrCode, String patientName, String address) {
        if (!hasText(mrCode) && !hasText(patientName) && !hasText(address)) {
            throw new IllegalArgumentException("Salah satu field pencarian pasien harus diisi.");
        }

        return jdbcTemplate.query(
            "select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, pat.v_patient_main_addr "
                + "from tb_medical_record mr "
                + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                + "join tb_registration reg on reg.n_mr_id = mr.n_mr_id "
                + "where reg.reg_status = ? "
                + "and mr.v_mr_code like ? "
                + "and upper(pat.v_patient_name) like ? "
                + "and upper(pat.v_patient_main_addr) like ? "
                + "order by mr.v_mr_code "
                + "limit 100",
            (resultSet, rowNum) -> new ApotikRegisteredPatientResponse(
                resultSet.getInt("n_mr_id"),
                resultSet.getString("v_mr_code"),
                resultSet.getString("v_patient_name"),
                resultSet.getString("v_patient_main_addr")
            ),
            REG_ACTIVE,
            likeMrCode(mrCode),
            likeUpper(patientName),
            likeUpper(address)
        );
    }

    public ApotikPatientDetailResponse getRegisteredPatientDetail(String mrCode) {
        RegisteredContext context = findRegisteredContext(mrCode);
        String tariffClass = determineTariffClass(context.registrationCode, context.registrationId);

        return jdbcTemplate.queryForObject(
            "select pat.n_patient_id, pat.n_patient_type_id, pat.v_patient_name, pat.v_patient_gender, pat.d_patient_dob, "
                + "pat.v_patient_main_addr "
                + "from ms_patient pat "
                + "where pat.n_patient_id = ?",
            (resultSet, rowNum) -> new ApotikPatientDetailResponse(
                context.patientId,
                context.mrId,
                context.mrCode,
                context.registrationId,
                context.registrationCode,
                getNullableInteger(resultSet, "n_patient_type_id"),
                resultSet.getString("v_patient_name"),
                resultSet.getString("v_patient_gender"),
                toIsoDate(resultSet.getDate("d_patient_dob")),
                resultSet.getString("v_patient_main_addr"),
                isInpatientRegistration(context.registrationCode),
                tariffClass
            ),
            context.patientId
        );
    }

    public List<ApotikItemOptionResponse> searchItems(Integer unitId, String code, String name, String tariffClass) {
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        if (warehouseId == null) {
            throw new IllegalStateException("Unit apotik belum terhubung dengan gudang inventory.");
        }
        if (!hasText(code) && !hasText(name)) {
            throw new IllegalArgumentException("Salah satu field pencarian item harus diisi.");
        }

        String effectiveTariffClass = normalizeTariffClass(tariffClass);
        return jdbcTemplate.query(
            "select inv.n_item_id as id, item.v_item_code as code, item.v_item_name as name, "
                + "item.n_r as jasa_r, sell.n_selling_price as harga, sat.v_mitem_end_quantify as satuan, "
                + "sum(inv.n_item_inv_qty) as jumlah, item.n_type as tipe "
                + "from tb_item_inventory inv "
                + "join ms_item item on item.n_item_id = inv.n_item_id "
                + "join ms_item_selling_price sell on sell.n_item_id = item.n_item_id "
                + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                + "join ms_treatment_class tclass on tclass.n_tclass_id = sell.n_tclass_id "
                + "where inv.n_whouse_id = ? "
                + "and inv.n_item_inv_qty > 0 "
                + "and item.v_item_code like ? "
                + "and upper(item.v_item_name) like ? "
                + "and tclass.v_tclass_desc = ? "
                + "group by id, code, name, jasa_r, harga, satuan, tipe "
                + "order by name "
                + "limit 100",
            (resultSet, rowNum) -> new ApotikItemOptionResponse(
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
            effectiveTariffClass
        );
    }

    public List<ApotikNoteSummaryResponse> searchNotes(Integer unitId, String noteNumber, String patientName) {
        return jdbcTemplate.query(
            "select note.n_exam_id, note.v_note_no, note.n_exam_status, pat.v_patient_name, note.d_whn_create "
                + "from tb_examination note "
                + "join ms_patient pat on pat.n_patient_id = note.n_patient_id "
                + "where note.n_unit_id = ? "
                + "and note.v_note_no like ? "
                + "and upper(pat.v_patient_name) like ? "
                + "and note.n_exam_status in (?, ?) "
                + "order by note.d_whn_create desc "
                + "limit 100",
            (resultSet, rowNum) -> new ApotikNoteSummaryResponse(
                resultSet.getInt("n_exam_id"),
                resultSet.getString("v_note_no"),
                resultSet.getString("v_patient_name"),
                resultSet.getInt("n_exam_status"),
                toStatusLabel(resultSet.getInt("n_exam_status")),
                toInstantString(resultSet.getTimestamp("d_whn_create"))
            ),
            unitId,
            likeRaw(noteNumber),
            likeUpper(patientName),
            NOTE_ACTIVE,
            NOTE_VALIDATED
        );
    }

    public ApotikNoteDetailResponse getNoteDetail(Integer noteId) {
        ApotikNoteHeader header = findNoteHeader(noteId);
        List<ApotikNoteLineResponse> lines = new ArrayList<ApotikNoteLineResponse>();
        lines.addAll(getItemLines(noteId));
        lines.addAll(getCompoundLines(noteId));
        lines.addAll(getMiscLines(noteId));

        return new ApotikNoteDetailResponse(
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
            header.receiptNumber,
            header.inpatient,
            header.tariffClass,
            header.cancelationNote,
            header.statusCode == NOTE_ACTIVE,
            header.statusCode == NOTE_ACTIVE,
            header.statusCode == NOTE_ACTIVE || header.statusCode == NOTE_VALIDATED,
            lines
        );
    }

    @Transactional
    public ApotikSaveResultResponse createNote(@Valid ApotikSaveRequest request, String username) {
        validateSaveRequest(request);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        UnitRow unit = findUnit(request.getUnitId());
        Integer patientId;
        Integer registrationId = null;
        String registrationCode = null;
        String medicalRecordCode = null;
        String tariffClass = DEFAULT_TARIFF_CLASS;

        if (Boolean.TRUE.equals(request.getReferencePatient())) {
            patientId = nextSequenceValue("ms_patient_n_patient_id_seq");
            insertReferencePatient(patientId, request, username, now);
        } else {
            RegisteredContext context = findRegisteredContext(request.getExistingMrCode());
            patientId = context.patientId;
            registrationId = context.registrationId;
            registrationCode = context.registrationCode;
            medicalRecordCode = context.mrCode;
            tariffClass = determineTariffClass(context.registrationCode, context.registrationId);
        }

        Integer noteId = nextSequenceValue("tb_examination_n_exam_id_seq");
        Integer noteSequence = nextSequenceValue("nota_rajal_seq");
        String noteNumber = generateNotaNumber(noteSequence, now, unit.unitCode);
        double totalAmount = calculateHeaderTotal(request.getLines(), unit.unitId, tariffClass);

        jdbcTemplate.update(
            "insert into tb_examination (n_exam_id, n_reg_id, n_patient_id, n_unit_id, v_note_no, n_total_amount, "
                + "n_payment_status, n_exam_status, v_who_create, d_whn_create, v_recipe_no) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            noteId,
            registrationId,
            patientId,
            unit.unitId,
            noteNumber,
            totalAmount,
            PAYMENT_UNPAID,
            NOTE_ACTIVE,
            normalizeUpper(username),
            now,
            normalizeTrim(request.getReceiptNumber())
        );

        persistLines(noteId, unit, request.getLines(), tariffClass, username, now);

        return new ApotikSaveResultResponse(
            noteId,
            noteNumber,
            NOTE_ACTIVE,
            toStatusLabel(NOTE_ACTIVE),
            medicalRecordCode,
            registrationCode
        );
    }

    @Transactional
    public ApotikSaveResultResponse updateNote(Integer noteId, @Valid ApotikSaveRequest request, String username) {
        validateSaveRequest(request);
        ApotikNoteHeader header = findNoteHeader(noteId);

        if (header.statusCode != NOTE_ACTIVE) {
            throw new IllegalStateException("Hanya nota status BARU yang bisa diubah.");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        UnitRow unit = findUnit(header.unitId);
        String tariffClass = DEFAULT_TARIFF_CLASS;

        if (header.registrationId == null && !Boolean.TRUE.equals(request.getReferencePatient())) {
            throw new IllegalArgumentException("Nota pasien bebas tidak bisa diubah menjadi pasien registrasi.");
        }
        if (header.registrationId != null && Boolean.TRUE.equals(request.getReferencePatient())) {
            throw new IllegalArgumentException("Nota pasien registrasi tidak bisa diubah menjadi pasien bebas.");
        }

        if (header.registrationId == null) {
            updateReferencePatient(header.patientId, request, username, now);
        } else {
            RegisteredContext context = findRegisteredContext(request.getExistingMrCode());
            if (!header.registrationId.equals(context.registrationId)) {
                throw new IllegalArgumentException("Nota harus tetap terhubung dengan registrasi pasien yang sama.");
            }
            tariffClass = determineTariffClass(context.registrationCode, context.registrationId);
        }

        restoreInventoryForItems(noteId, header.unitId);
        deleteNoteLineTables(noteId);

        double totalAmount = calculateHeaderTotal(request.getLines(), unit.unitId, header.registrationId == null ? DEFAULT_TARIFF_CLASS : tariffClass);
        jdbcTemplate.update(
            "update tb_examination set n_total_amount = ?, v_recipe_no = ?, v_who_change = ?, d_whn_change = ? where n_exam_id = ?",
            totalAmount,
            normalizeTrim(request.getReceiptNumber()),
            normalizeUpper(username),
            now,
            noteId
        );

        persistLines(noteId, unit, request.getLines(), header.registrationId == null ? DEFAULT_TARIFF_CLASS : tariffClass, username, now);

        return new ApotikSaveResultResponse(
            noteId,
            header.noteNumber,
            NOTE_ACTIVE,
            toStatusLabel(NOTE_ACTIVE),
            header.medicalRecordCode,
            header.registrationCode
        );
    }

    @Transactional
    public ApotikActionResultResponse validateNote(Integer noteId, String username) {
        ApotikNoteHeader header = findNoteHeader(noteId);
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

        return new ApotikActionResultResponse(noteId, header.noteNumber, NOTE_VALIDATED, toStatusLabel(NOTE_VALIDATED));
    }

    @Transactional
    public ApotikActionResultResponse cancelNote(Integer noteId, @Valid ApotikCancelRequest request, String username) {
        ApotikNoteHeader header = findNoteHeader(noteId);
        if (header.statusCode != NOTE_ACTIVE && header.statusCode != NOTE_VALIDATED) {
            throw new IllegalStateException("Status nota saat ini tidak bisa dibatalkan.");
        }

        restoreInventoryForItems(noteId, header.unitId);

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

        return new ApotikActionResultResponse(noteId, header.noteNumber, nextStatus, toStatusLabel(nextStatus));
    }

    public List<ApotikReturnSummaryResponse> searchReturns(String returnNumber, String patientName, String startDate, String endDate) {
        Timestamp startTimestamp = parseStartDate(startDate);
        Timestamp endTimestamp = parseEndDateExclusive(endDate);

        return jdbcTemplate.query(
            "select retur.n_retur_id, retur.v_retur_code, retur.n_status, retur.n_trx_value, retur.d_whn_create, "
                + "pat.v_patient_name, note.v_note_no "
                + "from tb_retur_pharmacy_trx retur "
                + "join ms_patient pat on pat.n_patient_id = retur.n_patient_id "
                + "left join tb_examination note on note.n_exam_id = retur.n_exam_id "
                + "where retur.v_retur_code like ? "
                + "and upper(pat.v_patient_name) like ? "
                + "and retur.n_status != ? "
                + "and retur.d_whn_create >= ? "
                + "and retur.d_whn_create < ? "
                + "order by retur.d_whn_create desc "
                + "limit 100",
            (resultSet, rowNum) -> new ApotikReturnSummaryResponse(
                resultSet.getInt("n_retur_id"),
                resultSet.getString("v_retur_code"),
                resultSet.getString("v_note_no"),
                resultSet.getString("v_patient_name"),
                resultSet.getDouble("n_trx_value"),
                resultSet.getInt("n_status"),
                toStatusLabel(resultSet.getInt("n_status")),
                toInstantString(resultSet.getTimestamp("d_whn_create"))
            ),
            likeRaw(returnNumber),
            likeUpper(patientName),
            NOTE_CANCELED,
            startTimestamp,
            endTimestamp
        );
    }

    public ApotikReturnDetailResponse getReturnDetail(Integer returnId) {
        ApotikReturnHeader header = findReturnHeader(returnId);
        return new ApotikReturnDetailResponse(
            header.returnId,
            header.returnNumber,
            header.statusCode,
            toStatusLabel(header.statusCode),
            header.totalAmount,
            header.createdAt,
            header.patientName,
            header.medicalRecordCode,
            header.registrationCode,
            header.originalNoteNumber,
            getReturnLines(returnId)
        );
    }

    private List<ApotikUnitResponse> getUnits(String username) {
        return jdbcTemplate.query(
            "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                + "from ms_user usr "
                + "join ms_staff staff on staff.n_staff_id = usr.n_staff_id "
                + "join ms_staff_in_unit stfunit on stfunit.n_staff_id = staff.n_staff_id "
                + "join ms_unit unt on unt.n_unit_id = stfunit.n_unit_id "
                + "where upper(usr.v_user_name) = ? "
                + "and staff.d_staff_fired_date is null "
                + "and unt.n_whouse_id is not null "
                + "order by unt.v_unit_name",
            (resultSet, rowNum) -> new ApotikUnitResponse(
                resultSet.getInt("n_unit_id"),
                resultSet.getString("v_unit_code"),
                resultSet.getString("v_unit_name"),
                getNullableInteger(resultSet, "n_whouse_id")
            ),
            normalizeUpper(username)
        );
    }

    private List<ApotikPatientTypeResponse> getPatientTypes() {
        return jdbcTemplate.query(
            "select n_patient_type_id, v_tpatient, v_tpatient_desc from ms_patient_type order by v_tpatient",
            (resultSet, rowNum) -> new ApotikPatientTypeResponse(
                resultSet.getInt("n_patient_type_id"),
                resultSet.getString("v_tpatient"),
                resultSet.getString("v_tpatient_desc")
            )
        );
    }

    private void persistLines(
        Integer noteId,
        UnitRow unit,
        List<ApotikLineItemRequest> lines,
        String tariffClass,
        String username,
        Timestamp now
    ) {
        for (ApotikLineItemRequest line : lines) {
            String type = normalizeUpper(line.getLineType());
            if (LINE_TYPE_ITEM.equals(type)) {
                persistItemLine(noteId, unit, line, tariffClass, username, now);
            } else if (LINE_TYPE_COMPOUND.equals(type)) {
                persistCompoundLine(noteId, unit, line, tariffClass, username, now);
            } else if (LINE_TYPE_MISC.equals(type)) {
                persistMiscLine(noteId, line, username, now);
            } else {
                throw new IllegalArgumentException("Jenis baris transaksi tidak dikenali: " + line.getLineType());
            }
        }
    }

    private void persistItemLine(
        Integer noteId,
        UnitRow unit,
        ApotikLineItemRequest line,
        String tariffClass,
        String username,
        Timestamp now
    ) {
        ItemRow item = findItem(line.getReferenceId(), unit.unitId, tariffClass);
        double quantity = requirePositive(line.getQuantity(), "Jumlah item harus lebih besar dari nol.");
        double amount = item.price * quantity;
        double discountAmount = calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
        double subtotal = amount - discountAmount;
        double remaining = quantity;

        List<InventoryRow> inventories = findInventories(unit.warehouseId, item.itemId);
        double perUnitDiscount = quantity == 0 ? 0 : discountAmount / quantity;
        double perUnitSubtotal = quantity == 0 ? 0 : subtotal / quantity;

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
                normalizeUpper(line.getInstruction())
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

    private void persistMiscLine(Integer noteId, ApotikLineItemRequest line, String username, Timestamp now) {
        short quantity = toShort(line.getQuantity());
        double unitPrice = requireNonNegative(line.getUnitPrice(), "Harga biaya lain-lain wajib diisi.");
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

    private void persistCompoundLine(
        Integer noteId,
        UnitRow unit,
        ApotikLineItemRequest line,
        String tariffClass,
        String username,
        Timestamp now
    ) {
        short quantity = toShort(line.getQuantity());
        CompoundLineAmounts amounts = calculateCompoundLineAmounts(line, unit.unitId, tariffClass);
        Integer compoundId = nextSequenceValue("tb_drug_ingredients_n_dingr_id_seq");
        String compoundCode = generateCompoundCode(compoundId, now);

        jdbcTemplate.update(
            "insert into tb_drug_ingredients (n_dingr_id, n_note_id, v_dingr_id, n_dingr_qty, n_er, n_dingr_quantify, "
                + "v_who_create, d_whn_create, aturan_pakai, n_amount_trx, v_disc_type, v_item_composition, "
                + "n_disc_amount, n_amount_after_disc) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            compoundId,
            noteId,
            compoundCode,
            quantity,
            Short.valueOf((short) 0),
            normalizeUpper(line.getUnitName()),
            normalizeUpper(username),
            now,
            normalizeUpper(line.getInstruction()),
            amounts.amount,
            normalizeDiscountType(line.getDiscountType()),
            buildCompoundDescription(line, unit.unitId, tariffClass),
            amounts.discountAmount,
            amounts.subtotal
        );

        for (ApotikCompoundComponentRequest component : line.getComponents()) {
            double componentQuantity = requirePositive(component.getQuantity(), "Jumlah komposisi racikan harus lebih besar dari nol.");
            ItemRow item = findItem(component.getReferenceId(), unit.unitId, tariffClass);
            double remaining = componentQuantity * quantity;
            List<InventoryRow> inventories = findInventories(unit.warehouseId, item.itemId);

            for (InventoryRow inventory : inventories) {
                if (remaining <= 0) {
                    break;
                }

                double picked = Math.min(remaining, inventory.quantity);
                Integer detailId = nextSequenceValue("tb_drug_ingredients_detail_n_dingr_det_id_seq");
                jdbcTemplate.update(
                    "insert into tb_drug_ingredients_detail (n_dingr_det_id, n_item_id, n_batch_id, n_dingr_id, n_dingr_det_qty, "
                        + "v_dingr_det_quantify, v_who_create, d_whn_create) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                    detailId,
                    item.itemId,
                    inventory.batchId,
                    compoundId,
                    picked,
                    item.unitName,
                    normalizeUpper(username),
                    now
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
                throw new IllegalStateException("Stok item " + item.itemCode + " untuk racikan tidak mencukupi.");
            }
        }
    }

    private void restoreInventoryForItems(Integer noteId, Integer unitId) {
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        if (warehouseId == null) {
            return;
        }

        List<InventoryRestoreRow> rows = jdbcTemplate.query(
            "select n_item_id, n_batch_id, n_qty from tb_item_trx where n_note_id = ?",
            (resultSet, rowNum) -> new InventoryRestoreRow(
                resultSet.getInt("n_item_id"),
                resultSet.getInt("n_batch_id"),
                resultSet.getDouble("n_qty")
            ),
            noteId
        );

        for (InventoryRestoreRow row : rows) {
            restoreInventory(row.itemId, row.batchId, warehouseId, row.quantity);
        }

        List<InventoryRestoreRow> compoundRows = jdbcTemplate.query(
            "select det.n_item_id, det.n_batch_id, det.n_dingr_det_qty "
                + "from tb_drug_ingredients_detail det "
                + "join tb_drug_ingredients hdr on hdr.n_dingr_id = det.n_dingr_id "
                + "where hdr.n_note_id = ?",
            (resultSet, rowNum) -> new InventoryRestoreRow(
                resultSet.getInt("n_item_id"),
                resultSet.getInt("n_batch_id"),
                resultSet.getDouble("n_dingr_det_qty")
            ),
            noteId
        );

        for (InventoryRestoreRow row : compoundRows) {
            restoreInventory(row.itemId, row.batchId, warehouseId, row.quantity);
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
        jdbcTemplate.update(
            "delete from tb_drug_ingredients_detail where n_dingr_id in (select n_dingr_id from tb_drug_ingredients where n_note_id = ?)",
            noteId
        );
        jdbcTemplate.update("delete from tb_drug_ingredients where n_note_id = ?", noteId);
        jdbcTemplate.update("delete from tb_misc_trx where n_note_id = ?", noteId);
    }

    private void validateSaveRequest(ApotikSaveRequest request) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("Minimal satu baris transaksi apotik harus diisi.");
        }

        if (Boolean.TRUE.equals(request.getReferencePatient())) {
            if (!hasText(request.getPatientName()) || !hasText(request.getBirthDate()) || !hasText(request.getAddress())) {
                throw new IllegalArgumentException("Pasien bebas wajib mengisi nama, tanggal lahir, dan alamat.");
            }
        } else if (!hasText(request.getExistingMrCode())) {
            throw new IllegalArgumentException("Pasien terdaftar wajib memilih No.MR.");
        }

        for (ApotikLineItemRequest line : request.getLines()) {
            String type = normalizeUpper(line.getLineType());
            if (LINE_TYPE_ITEM.equals(type)) {
                if (line.getReferenceId() == null) {
                    throw new IllegalArgumentException("Master item wajib dipilih.");
                }
                if (!hasText(line.getInstruction())) {
                    throw new IllegalArgumentException("Aturan pakai wajib diisi untuk setiap item obat.");
                }
            } else if (LINE_TYPE_COMPOUND.equals(type)) {
                if (!hasText(line.getDescription())) {
                    throw new IllegalArgumentException("Nama racikan wajib diisi.");
                }
                if (!hasText(line.getUnitName())) {
                    throw new IllegalArgumentException("Satuan racikan wajib diisi.");
                }
                if (!hasText(line.getInstruction())) {
                    throw new IllegalArgumentException("Aturan pakai wajib diisi untuk setiap racikan.");
                }
                if (line.getComponents() == null || line.getComponents().isEmpty()) {
                    throw new IllegalArgumentException("Komposisi racikan wajib diisi minimal satu item.");
                }
                for (ApotikCompoundComponentRequest component : line.getComponents()) {
                    if (component.getReferenceId() == null) {
                        throw new IllegalArgumentException("Master item komposisi racikan wajib dipilih.");
                    }
                    requirePositive(component.getQuantity(), "Jumlah komposisi racikan harus lebih besar dari nol.");
                }
            } else if (LINE_TYPE_MISC.equals(type)) {
                if (!hasText(line.getDescription())) {
                    throw new IllegalArgumentException("Nama biaya lain-lain wajib diisi.");
                }
                requireNonNegative(line.getUnitPrice(), "Harga biaya lain-lain wajib diisi.");
            }

            requirePositive(line.getQuantity(), "Jumlah transaksi harus lebih besar dari nol.");
        }
    }

    private double calculateHeaderTotal(List<ApotikLineItemRequest> lines, Integer unitId, String tariffClass) {
        double total = 0;
        for (ApotikLineItemRequest line : lines) {
            String type = normalizeUpper(line.getLineType());
            if (LINE_TYPE_ITEM.equals(type)) {
                ItemRow item = findItem(line.getReferenceId(), unitId, tariffClass);
                double amount = item.price * line.getQuantity().doubleValue();
                total += amount - calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
            } else if (LINE_TYPE_COMPOUND.equals(type)) {
                total += calculateCompoundLineAmounts(line, unitId, tariffClass).subtotal;
            } else if (LINE_TYPE_MISC.equals(type)) {
                double amount = requireNonNegative(line.getUnitPrice(), "Harga biaya lain-lain wajib diisi.") * line.getQuantity().doubleValue();
                total += amount - calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
            }
        }
        return total;
    }

    private List<ApotikNoteLineResponse> getItemLines(Integer noteId) {
        return jdbcTemplate.query(
            "select trx.n_item_id as id, item.v_item_code as code, item.v_item_name as name, sat.v_mitem_end_quantify as satuan, "
                + "sum(trx.n_qty) as qty, sum(trx.n_amount_trx) as value, sum(trx.n_disc_amount) as discount, "
                + "sum(trx.n_amount_after_disc) as total, max(trx.v_disc_type) as disc_type, max(trx.aturan_pakai) as aturan "
                + "from tb_item_trx trx "
                + "join ms_item item on item.n_item_id = trx.n_item_id "
                + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                + "where trx.n_note_id = ? "
                + "group by id, code, name, satuan "
                + "order by name",
            (resultSet, rowNum) -> {
                double quantity = resultSet.getDouble("qty");
                double amount = resultSet.getDouble("value");
                return new ApotikNoteLineResponse(
                    LINE_TYPE_ITEM,
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
                    resultSet.getString("aturan"),
                    null
                );
            },
            noteId
        );
    }

    private List<ApotikNoteLineResponse> getCompoundLines(Integer noteId) {
        return jdbcTemplate.query(
            "select n_dingr_id, v_dingr_id, v_item_composition, n_dingr_qty, n_dingr_quantify, n_amount_trx, "
                + "n_disc_amount, v_disc_type, n_amount_after_disc, aturan_pakai "
                + "from tb_drug_ingredients where n_note_id = ? order by n_dingr_id",
            (resultSet, rowNum) -> {
                double quantity = resultSet.getDouble("n_dingr_qty");
                double amount = resultSet.getDouble("n_amount_trx");
                Integer compoundId = resultSet.getInt("n_dingr_id");
                return new ApotikNoteLineResponse(
                    LINE_TYPE_COMPOUND,
                    compoundId,
                    compoundId,
                    resultSet.getString("v_dingr_id"),
                    resultSet.getString("v_item_composition"),
                    quantity,
                    resultSet.getString("n_dingr_quantify"),
                    quantity == 0 ? 0 : amount / quantity,
                    normalizeDiscountType(resultSet.getString("v_disc_type")),
                    resultSet.getDouble("n_disc_amount"),
                    resultSet.getDouble("n_amount_after_disc"),
                    resultSet.getString("aturan_pakai"),
                    getCompoundComponents(compoundId, quantity)
                );
            },
            noteId
        );
    }

    private List<ApotikNoteLineResponse> getMiscLines(Integer noteId) {
        return jdbcTemplate.query(
            "select n_misc_trx_id, v_misc_name, n_qty, n_item_price, n_disc_amount, v_disc_type, n_amount_after_disc "
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
                null,
                null
            ),
            noteId
        );
    }

    private List<ApotikCompoundComponentResponse> getCompoundComponents(Integer compoundId, double compoundQuantity) {
        return jdbcTemplate.query(
            "select det.n_item_id, item.v_item_code, item.v_item_name, sat.v_mitem_end_quantify, sum(det.n_dingr_det_qty) as qty "
                + "from tb_drug_ingredients_detail det "
                + "join ms_item item on item.n_item_id = det.n_item_id "
                + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                + "where det.n_dingr_id = ? "
                + "group by det.n_item_id, item.v_item_code, item.v_item_name, sat.v_mitem_end_quantify "
                + "order by item.v_item_name",
            (resultSet, rowNum) -> new ApotikCompoundComponentResponse(
                resultSet.getInt("n_item_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_mitem_end_quantify"),
                compoundQuantity <= 0 ? 0 : resultSet.getDouble("qty") / compoundQuantity
            ),
            compoundId
        );
    }

    private ApotikNoteHeader findNoteHeader(Integer noteId) {
        try {
            return jdbcTemplate.queryForObject(
                "select note.n_exam_id, note.v_note_no, note.n_exam_status, note.n_total_amount, note.n_unit_id, unit.v_unit_code, "
                    + "unit.v_unit_name, note.n_patient_id, pat.n_patient_type_id, pat.v_patient_name, pat.v_patient_gender, "
                    + "pat.d_patient_dob, pat.v_patient_main_addr, note.v_cancelation_note, note.v_recipe_no, "
                    + "reg.n_reg_id, reg.v_reg_secondary_id, mr.v_mr_code "
                    + "from tb_examination note "
                    + "join ms_unit unit on unit.n_unit_id = note.n_unit_id "
                    + "join ms_patient pat on pat.n_patient_id = note.n_patient_id "
                    + "left join tb_registration reg on reg.n_reg_id = note.n_reg_id "
                    + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                    + "where note.n_exam_id = ?",
                (resultSet, rowNum) -> {
                    String registrationCode = resultSet.getString("v_reg_secondary_id");
                    Integer registrationId = getNullableInteger(resultSet, "n_reg_id");
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
                        registrationId,
                        registrationCode,
                        resultSet.getString("v_recipe_no"),
                        isInpatientRegistration(registrationCode),
                        determineTariffClass(registrationCode, registrationId),
                        resultSet.getString("v_cancelation_note")
                    );
                },
                noteId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Nota apotik tidak ditemukan.");
        }
    }

    private RegisteredContext findRegisteredContext(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("No.MR pasien terdaftar wajib dipilih.");
        }

        try {
            return jdbcTemplate.queryForObject(
                "select mr.n_mr_id, mr.v_mr_code, mr.n_patient_id, reg.n_reg_id, reg.v_reg_secondary_id "
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
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Registrasi pasien aktif tidak ditemukan.");
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
            throw new IllegalArgumentException("Unit apotik tidak ditemukan.");
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

    private ItemRow findItem(Integer itemId, Integer unitId, String tariffClass) {
        Integer warehouseId = findWarehouseIdByUnit(unitId);
        if (warehouseId == null) {
            throw new IllegalStateException("Gudang unit tidak ditemukan.");
        }

        try {
            return jdbcTemplate.queryForObject(
                "select item.n_item_id, item.v_item_code, item.v_item_name, sell.n_selling_price "
                    + ", sat.v_mitem_end_quantify "
                    + "from ms_item item "
                    + "join ms_item_selling_price sell on sell.n_item_id = item.n_item_id "
                    + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                    + "join ms_treatment_class tclass on tclass.n_tclass_id = sell.n_tclass_id "
                    + "where item.n_item_id = ? "
                    + "and tclass.v_tclass_desc = ? "
                    + "and exists (select 1 from tb_item_inventory inv where inv.n_whouse_id = ? and inv.n_item_id = item.n_item_id and inv.n_item_inv_qty > 0) "
                    + "limit 1",
                (resultSet, rowNum) -> new ItemRow(
                    resultSet.getInt("n_item_id"),
                    resultSet.getString("v_item_code"),
                    resultSet.getString("v_item_name"),
                    resultSet.getDouble("n_selling_price"),
                    resultSet.getString("v_mitem_end_quantify")
                ),
                itemId,
                normalizeTariffClass(tariffClass),
                warehouseId
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

    private void insertReferencePatient(Integer patientId, ApotikSaveRequest request, String username, Timestamp now) {
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

    private void updateReferencePatient(Integer patientId, ApotikSaveRequest request, String username, Timestamp now) {
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

    private String determineTariffClass(String registrationCode, Integer registrationId) {
        if (!isInpatientRegistration(registrationCode) || registrationId == null) {
            return DEFAULT_TARIFF_CLASS;
        }

        try {
            String tariffClass = jdbcTemplate.queryForObject(
                "select tclass.v_tclass_desc "
                    + "from tb_bed_occupancy boc "
                    + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                    + "join ms_treatment_class tclass on tclass.n_tclass_id = bed.n_tclass_id "
                    + "where boc.n_reg_primary_id = ? "
                    + "order by boc.d_whn_create desc limit 1",
                String.class,
                registrationId
            );
            return hasText(tariffClass) ? tariffClass : DEFAULT_TARIFF_CLASS;
        } catch (EmptyResultDataAccessException exception) {
            return DEFAULT_TARIFF_CLASS;
        }
    }

    private boolean isInpatientRegistration(String registrationCode) {
        return hasText(registrationCode) && registrationCode.trim().toUpperCase(Locale.ROOT).startsWith("I");
    }

    private List<ApotikReturnLineResponse> getReturnLines(Integer returnId) {
        return jdbcTemplate.query(
            "select det.n_retur_det_id, item.v_item_code, item.v_item_name, sat.v_mitem_end_quantify, "
                + "det.n_qty, det.n_value, det.n_total_qty "
                + "from tb_retur_pharmacy_detail_trx det "
                + "join ms_item item on item.n_item_id = det.n_item_id "
                + "join ms_item_measurement sat on sat.n_mitem_id = item.n_mitem_id "
                + "where det.n_retur_id = ? "
                + "order by item.v_item_name",
            (resultSet, rowNum) -> new ApotikReturnLineResponse(
                resultSet.getInt("n_retur_det_id"),
                resultSet.getString("v_item_code"),
                resultSet.getString("v_item_name"),
                resultSet.getString("v_mitem_end_quantify"),
                getNullableShort(resultSet, "n_total_qty"),
                getNullableShort(resultSet, "n_qty"),
                resultSet.getDouble("n_value")
            ),
            returnId
        );
    }

    private ApotikReturnHeader findReturnHeader(Integer returnId) {
        try {
            return jdbcTemplate.queryForObject(
                "select retur.n_retur_id, retur.v_retur_code, retur.n_status, retur.n_trx_value, retur.d_whn_create, "
                    + "pat.v_patient_name, reg.v_reg_secondary_id, mr.v_mr_code, note.v_note_no "
                    + "from tb_retur_pharmacy_trx retur "
                    + "join ms_patient pat on pat.n_patient_id = retur.n_patient_id "
                    + "left join tb_registration reg on reg.n_reg_id = retur.n_reg_id "
                    + "left join tb_medical_record mr on mr.n_mr_id = reg.n_mr_id "
                    + "left join tb_examination note on note.n_exam_id = retur.n_exam_id "
                    + "where retur.n_retur_id = ?",
                (resultSet, rowNum) -> new ApotikReturnHeader(
                    resultSet.getInt("n_retur_id"),
                    resultSet.getString("v_retur_code"),
                    resultSet.getInt("n_status"),
                    resultSet.getDouble("n_trx_value"),
                    toInstantString(resultSet.getTimestamp("d_whn_create")),
                    resultSet.getString("v_patient_name"),
                    resultSet.getString("v_mr_code"),
                    resultSet.getString("v_reg_secondary_id"),
                    resultSet.getString("v_note_no")
                ),
                returnId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Nota retur apotik tidak ditemukan.");
        }
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

    private String generateCompoundCode(Integer compoundId, Timestamp timestamp) {
        return "R-" + NOTE_DATE_FORMAT.format(timestamp) + "-" + formatNoteSequence(compoundId % 1000000);
    }

    private String toIsoDate(Date date) {
        return date == null ? null : date.toLocalDate().format(ISO_DATE);
    }

    private String toInstantString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
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
        if (DISCOUNT_PERCENT.equals(normalizeDiscountType(discountType))) {
            return amount * value / 100d;
        }
        return value;
    }

    private CompoundLineAmounts calculateCompoundLineAmounts(ApotikLineItemRequest line, Integer unitId, String tariffClass) {
        double quantity = requirePositive(line.getQuantity(), "Jumlah racikan harus lebih besar dari nol.");
        double amount = 0;

        if (line.getComponents() == null || line.getComponents().isEmpty()) {
            throw new IllegalArgumentException("Komposisi racikan wajib diisi minimal satu item.");
        }

        for (ApotikCompoundComponentRequest component : line.getComponents()) {
            double componentQuantity = requirePositive(component.getQuantity(), "Jumlah komposisi racikan harus lebih besar dari nol.");
            ItemRow item = findItem(component.getReferenceId(), unitId, tariffClass);
            amount += item.price * componentQuantity * quantity;
        }

        double discountAmount = calculateDiscount(amount, line.getDiscountType(), line.getDiscountValue());
        return new CompoundLineAmounts(amount, discountAmount, amount - discountAmount);
    }

    private String buildCompoundDescription(ApotikLineItemRequest line, Integer unitId, String tariffClass) {
        if (hasText(line.getDescription())) {
            return normalizeUpper(line.getDescription());
        }

        List<String> componentDescriptions = new ArrayList<String>();
        for (ApotikCompoundComponentRequest component : line.getComponents()) {
            ItemRow item = findItem(component.getReferenceId(), unitId, tariffClass);
            componentDescriptions.add(item.itemName + " " + trimTrailingZero(component.getQuantity()) + " " + item.unitName);
        }
        return normalizeUpper(String.join(", ", componentDescriptions));
    }

    private String trimTrailingZero(Double value) {
        if (value == null) {
            return "0";
        }
        double numeric = value.doubleValue();
        if (numeric == Math.rint(numeric)) {
            return String.valueOf((long) numeric);
        }
        return String.valueOf(numeric);
    }

    private Timestamp parseStartDate(String value) {
        LocalDate date = hasText(value) ? LocalDate.parse(value, ISO_DATE) : LocalDate.now().minusDays(30);
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private Timestamp parseEndDateExclusive(String value) {
        LocalDate date = hasText(value) ? LocalDate.parse(value, ISO_DATE) : LocalDate.now();
        return Timestamp.valueOf(date.plusDays(1).atStartOfDay());
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

    private String normalizeTrim(String value) {
        return value == null ? null : value.trim();
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
        if (value == null || value.doubleValue() < 0) {
            throw new IllegalArgumentException(message);
        }
        return value.doubleValue();
    }

    private double requirePositive(Number value, String message) {
        if (value == null || value.doubleValue() <= 0) {
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

        private RegisteredContext(Integer mrId, String mrCode, Integer patientId, Integer registrationId, String registrationCode) {
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientId = patientId;
            this.registrationId = registrationId;
            this.registrationCode = registrationCode;
        }
    }

    private static final class ItemRow {
        private final Integer itemId;
        private final String itemCode;
        private final String itemName;
        private final double price;
        private final String unitName;

        private ItemRow(Integer itemId, String itemCode, String itemName, double price, String unitName) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.price = price;
            this.unitName = unitName;
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

    private static final class CompoundLineAmounts {
        private final double amount;
        private final double discountAmount;
        private final double subtotal;

        private CompoundLineAmounts(double amount, double discountAmount, double subtotal) {
            this.amount = amount;
            this.discountAmount = discountAmount;
            this.subtotal = subtotal;
        }
    }

    private static final class ApotikNoteHeader {
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
        private final String receiptNumber;
        private final boolean inpatient;
        private final String tariffClass;
        private final String cancelationNote;

        private ApotikNoteHeader(
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
            String receiptNumber,
            boolean inpatient,
            String tariffClass,
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
            this.receiptNumber = receiptNumber;
            this.inpatient = inpatient;
            this.tariffClass = tariffClass;
            this.cancelationNote = cancelationNote;
        }
    }

    private static final class ApotikReturnHeader {
        private final Integer returnId;
        private final String returnNumber;
        private final Integer statusCode;
        private final double totalAmount;
        private final String createdAt;
        private final String patientName;
        private final String medicalRecordCode;
        private final String registrationCode;
        private final String originalNoteNumber;

        private ApotikReturnHeader(
            Integer returnId,
            String returnNumber,
            Integer statusCode,
            double totalAmount,
            String createdAt,
            String patientName,
            String medicalRecordCode,
            String registrationCode,
            String originalNoteNumber
        ) {
            this.returnId = returnId;
            this.returnNumber = returnNumber;
            this.statusCode = statusCode;
            this.totalAmount = totalAmount;
            this.createdAt = createdAt;
            this.patientName = patientName;
            this.medicalRecordCode = medicalRecordCode;
            this.registrationCode = registrationCode;
            this.originalNoteNumber = originalNoteNumber;
        }
    }
}

class ApotikMastersResponse {
    private final List<ApotikUnitResponse> units;
    private final List<ApotikPatientTypeResponse> patientTypes;

    ApotikMastersResponse(List<ApotikUnitResponse> units, List<ApotikPatientTypeResponse> patientTypes) {
        this.units = units;
        this.patientTypes = patientTypes;
    }

    public List<ApotikUnitResponse> getUnits() {
        return units;
    }

    public List<ApotikPatientTypeResponse> getPatientTypes() {
        return patientTypes;
    }
}

class ApotikUnitResponse {
    private final Integer unitId;
    private final String unitCode;
    private final String unitName;
    private final Integer warehouseId;

    ApotikUnitResponse(Integer unitId, String unitCode, String unitName, Integer warehouseId) {
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

class ApotikPatientTypeResponse {
    private final Integer patientTypeId;
    private final String patientTypeCode;
    private final String patientTypeName;

    ApotikPatientTypeResponse(Integer patientTypeId, String patientTypeCode, String patientTypeName) {
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

class ApotikRegisteredPatientResponse {
    private final Integer medicalRecordId;
    private final String medicalRecordCode;
    private final String patientName;
    private final String address;

    ApotikRegisteredPatientResponse(Integer medicalRecordId, String medicalRecordCode, String patientName, String address) {
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

class ApotikPatientDetailResponse {
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
    private final boolean inpatient;
    private final String tariffClass;

    ApotikPatientDetailResponse(
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
        boolean inpatient,
        String tariffClass
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
        this.inpatient = inpatient;
        this.tariffClass = tariffClass;
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

    public boolean isInpatient() {
        return inpatient;
    }

    public String getTariffClass() {
        return tariffClass;
    }
}

class ApotikItemOptionResponse {
    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final String unitName;
    private final double price;
    private final double stockQuantity;
    private final Short jasaR;
    private final Short itemType;

    ApotikItemOptionResponse(
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

class ApotikNoteSummaryResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final String patientName;
    private final Integer statusCode;
    private final String statusLabel;
    private final String createdAt;

    ApotikNoteSummaryResponse(
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

class ApotikNoteDetailResponse {
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
    private final String receiptNumber;
    private final boolean inpatient;
    private final String tariffClass;
    private final String cancelationNote;
    private final boolean canModify;
    private final boolean canValidate;
    private final boolean canCancel;
    private final List<ApotikNoteLineResponse> lines;

    ApotikNoteDetailResponse(
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
        String receiptNumber,
        boolean inpatient,
        String tariffClass,
        String cancelationNote,
        boolean canModify,
        boolean canValidate,
        boolean canCancel,
        List<ApotikNoteLineResponse> lines
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
        this.receiptNumber = receiptNumber;
        this.inpatient = inpatient;
        this.tariffClass = tariffClass;
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

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public boolean isInpatient() {
        return inpatient;
    }

    public String getTariffClass() {
        return tariffClass;
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

    public List<ApotikNoteLineResponse> getLines() {
        return lines;
    }
}

class ApotikNoteLineResponse {
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
    private final String instruction;
    private final List<ApotikCompoundComponentResponse> components;

    ApotikNoteLineResponse(
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
        String instruction,
        List<ApotikCompoundComponentResponse> components
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
        this.instruction = instruction;
        this.components = components;
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

    public String getInstruction() {
        return instruction;
    }

    public List<ApotikCompoundComponentResponse> getComponents() {
        return components;
    }
}

class ApotikCompoundComponentResponse {
    private final Integer referenceId;
    private final String code;
    private final String description;
    private final String unitName;
    private final double quantity;

    ApotikCompoundComponentResponse(Integer referenceId, String code, String description, String unitName, double quantity) {
        this.referenceId = referenceId;
        this.code = code;
        this.description = description;
        this.unitName = unitName;
        this.quantity = quantity;
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

    public String getUnitName() {
        return unitName;
    }

    public double getQuantity() {
        return quantity;
    }
}

class ApotikSaveResultResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final Integer statusCode;
    private final String statusLabel;
    private final String medicalRecordCode;
    private final String registrationCode;

    ApotikSaveResultResponse(
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

class ApotikActionResultResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final Integer statusCode;
    private final String statusLabel;

    ApotikActionResultResponse(Integer noteId, String noteNumber, Integer statusCode, String statusLabel) {
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

class ApotikReturnSummaryResponse {
    private final Integer returnId;
    private final String returnNumber;
    private final String originalNoteNumber;
    private final String patientName;
    private final double totalAmount;
    private final Integer statusCode;
    private final String statusLabel;
    private final String createdAt;

    ApotikReturnSummaryResponse(
        Integer returnId,
        String returnNumber,
        String originalNoteNumber,
        String patientName,
        double totalAmount,
        Integer statusCode,
        String statusLabel,
        String createdAt
    ) {
        this.returnId = returnId;
        this.returnNumber = returnNumber;
        this.originalNoteNumber = originalNoteNumber;
        this.patientName = patientName;
        this.totalAmount = totalAmount;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.createdAt = createdAt;
    }

    public Integer getReturnId() {
        return returnId;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public String getOriginalNoteNumber() {
        return originalNoteNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public double getTotalAmount() {
        return totalAmount;
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

class ApotikReturnDetailResponse {
    private final Integer returnId;
    private final String returnNumber;
    private final Integer statusCode;
    private final String statusLabel;
    private final double totalAmount;
    private final String createdAt;
    private final String patientName;
    private final String medicalRecordCode;
    private final String registrationCode;
    private final String originalNoteNumber;
    private final List<ApotikReturnLineResponse> lines;

    ApotikReturnDetailResponse(
        Integer returnId,
        String returnNumber,
        Integer statusCode,
        String statusLabel,
        double totalAmount,
        String createdAt,
        String patientName,
        String medicalRecordCode,
        String registrationCode,
        String originalNoteNumber,
        List<ApotikReturnLineResponse> lines
    ) {
        this.returnId = returnId;
        this.returnNumber = returnNumber;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.patientName = patientName;
        this.medicalRecordCode = medicalRecordCode;
        this.registrationCode = registrationCode;
        this.originalNoteNumber = originalNoteNumber;
        this.lines = lines;
    }

    public Integer getReturnId() {
        return returnId;
    }

    public String getReturnNumber() {
        return returnNumber;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getMedicalRecordCode() {
        return medicalRecordCode;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public String getOriginalNoteNumber() {
        return originalNoteNumber;
    }

    public List<ApotikReturnLineResponse> getLines() {
        return lines;
    }
}

class ApotikReturnLineResponse {
    private final Integer lineId;
    private final String itemCode;
    private final String itemName;
    private final String unitName;
    private final Short originalQuantity;
    private final Short returnedQuantity;
    private final double value;

    ApotikReturnLineResponse(
        Integer lineId,
        String itemCode,
        String itemName,
        String unitName,
        Short originalQuantity,
        Short returnedQuantity,
        double value
    ) {
        this.lineId = lineId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unitName = unitName;
        this.originalQuantity = originalQuantity;
        this.returnedQuantity = returnedQuantity;
        this.value = value;
    }

    public Integer getLineId() {
        return lineId;
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

    public Short getOriginalQuantity() {
        return originalQuantity;
    }

    public Short getReturnedQuantity() {
        return returnedQuantity;
    }

    public double getValue() {
        return value;
    }
}

class ApotikSaveRequest {
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
    private String receiptNumber;

    @NotEmpty
    private List<ApotikLineItemRequest> lines;

    public Integer getUnitId() {
        return unitId;
    }

    public Boolean getReferencePatient() {
        return referencePatient;
    }

    public String getExistingMrCode() {
        return existingMrCode;
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

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public List<ApotikLineItemRequest> getLines() {
        return lines;
    }
}

class ApotikLineItemRequest {
    @NotBlank
    private String lineType;

    private Integer referenceId;

    @NotNull
    @DecimalMin(value = "0.0001")
    private Double quantity;

    private Double unitPrice;
    private String discountType;
    private Double discountValue;
    private String description;
    private String unitName;
    private String instruction;
    private List<ApotikCompoundComponentRequest> components;

    public String getLineType() {
        return lineType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public Double getQuantity() {
        return quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public String getDiscountType() {
        return discountType;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public String getDescription() {
        return description;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getInstruction() {
        return instruction;
    }

    public List<ApotikCompoundComponentRequest> getComponents() {
        return components;
    }
}

class ApotikCompoundComponentRequest {
    @NotNull
    private Integer referenceId;

    @NotNull
    @DecimalMin(value = "0.0001")
    private Double quantity;

    public Integer getReferenceId() {
        return referenceId;
    }

    public Double getQuantity() {
        return quantity;
    }
}

class ApotikCancelRequest {
    @NotBlank
    private String reason;

    public String getReason() {
        return reason;
    }
}
