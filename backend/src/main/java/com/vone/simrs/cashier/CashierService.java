package com.vone.simrs.cashier;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.ward.WardUnitResponse;
import java.sql.Timestamp;
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
 * Service untuk screen SC0021 (TRANSAKSI KASIR / kasir.zul).
 *
 * <p>
 * Migrasi dari legacy {@code CashierTransactionController} +
 * {@code PaymentTermController} + {@code CashierManagerImpl} + {@code CashierDAO}
 * (saveBillNotas / saveDeposit / getBalancet).
 */
@Service
public class CashierService {

    private static final short BELUM_LUNAS = 0;
    private static final short SUDAH_LUNAS = 1;
    private static final short NOTE_VALIDATED = 2;
    private static final short BANK_SETTLEMENT = 1;
    private static final short INSURANCE_SETTLEMENT = 2;
    private static final short CASH_SETTLEMENT = 3;
    private static final short DEPOSIT_SETTLEMENT = 4;
    private static final DateTimeFormatter BILL_DATE = DateTimeFormatter.ofPattern("yyMM");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public CashierService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    // ------------------------------------------------------------------ masters

    public CashierMastersResponse getMasters(String username) {
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
        List<CashierBankResponse> banks = jdbcTemplate.query(
                "select n_bank_id, v_bank_name from ms_bank order by v_bank_name",
                (resultSet, rowNum) -> new CashierBankResponse(
                        resultSet.getInt("n_bank_id"),
                        resultSet.getString("v_bank_name")));
        List<CashierInsuranceResponse> insurances = jdbcTemplate.query(
                "select n_insurance_id, v_insurance_name from ms_insurance order by v_insurance_name",
                (resultSet, rowNum) -> new CashierInsuranceResponse(
                        resultSet.getInt("n_insurance_id"),
                        resultSet.getString("v_insurance_name")));
        return new CashierMastersResponse(units, banks, insurances);
    }

    // ------------------------------------------------------------------ pasien

    /** Cari pasien terdaftar (registrasi aktif). */
    public List<com.vone.simrs.ward.WardPatientOptionResponse> searchRegisteredPatients(
            String mrCode, String patientName, String address, String birthDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("select distinct mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, ")
                .append("pat.v_patient_main_addr, pat.n_patient_type_id ")
                .append("from tb_medical_record mr ")
                .append("join ms_patient pat on pat.n_patient_id = mr.n_patient_id ")
                .append("join tb_registration reg on reg.n_mr_id = mr.n_mr_id ")
                .append("where reg.reg_status = 1 and mr.v_mr_code like ? ")
                .append("and pat.v_patient_name like ? and pat.v_patient_main_addr like ? ");
        List<Object> params = new ArrayList<>();
        params.add(like(normalizeOptionalUpper(mrCode)));
        params.add(like(normalizeOptionalUpper(patientName)));
        params.add(like(normalizeOptionalUpper(address)));
        if (hasText(birthDate)) {
            sql.append("and pat.d_patient_dob = ? ");
            params.add(java.time.LocalDate.parse(birthDate));
        }
        sql.append("limit 100");
        return jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> {
                    Integer typeId = getNullableInteger(resultSet, "n_patient_type_id");
                    return new com.vone.simrs.ward.WardPatientOptionResponse(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("v_patient_name"),
                            typeId != null && typeId == 8 ? "BPJS" : "NON BPJS",
                            resultSet.getString("v_patient_main_addr"));
                });
    }

    /** Detail pasien: reg aktif + bed (jika ranap) + saldo deposit. */
    public CashierPatientDetailResponse getPatientDetail(String mrCode) {
        if (!hasText(mrCode)) {
            throw new IllegalArgumentException("NO. MR HARUS DI ISI!");
        }
        PatientRow patient = findPatientByMrCode(normalizeMrCode(mrCode));
        RegistrationRow reg = findLastRegistration(patient.mrId);
        if (reg == null) {
            throw new IllegalArgumentException("PASIEN BELUM TERDAFTAR!");
        }
        boolean ranap = reg.regNo != null && reg.regNo.startsWith("I");
        String bed = ranap ? findBedByRegId(reg.regId) : "";
        Double deposit = findDepositBalance(reg.regId);
        return new CashierPatientDetailResponse(
                patient.mrCode, reg.regId, reg.regNo, patient.patientName,
                patient.address, patient.patientTypeName, bed, ranap, deposit);
    }

    private PatientRow findPatientByMrCode(String mrCode) {
        try {
            return jdbcTemplate.queryForObject(
                    "select mr.n_mr_id, mr.v_mr_code, pat.v_patient_name, "
                            + "pat.v_patient_main_addr, pt.v_tpatient_desc "
                            + "from tb_medical_record mr "
                            + "join ms_patient pat on pat.n_patient_id = mr.n_patient_id "
                            + "left join ms_patient_type pt on pt.n_patient_type_id = pat.n_patient_type_id "
                            + "where upper(mr.v_mr_code) = ?",
                    (resultSet, rowNum) -> new PatientRow(
                            resultSet.getInt("n_mr_id"),
                            resultSet.getString("v_mr_code"),
                            resultSet.getString("v_patient_name"),
                            resultSet.getString("v_patient_main_addr"),
                            resultSet.getString("v_tpatient_desc")),
                    mrCode);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("MR TIDAK DITEMUKAN!");
        }
    }

    private RegistrationRow findLastRegistration(Integer mrId) {
        List<RegistrationRow> rows = jdbcTemplate.query(
                "select n_reg_id, v_reg_secondary_id from tb_registration "
                        + "where n_mr_id = ? and reg_status = 1 "
                        + "order by d_registration_date desc limit 1",
                (resultSet, rowNum) -> new RegistrationRow(
                        resultSet.getInt("n_reg_id"),
                        resultSet.getString("v_reg_secondary_id")),
                mrId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String findBedByRegId(Integer regId) {
        List<String> rows = jdbcTemplate.query(
                "select bed.v_bed_desc from tb_bed_occupancy boc "
                        + "join ms_bed bed on bed.n_bed_id = boc.n_bed_primary_id "
                        + "where boc.n_reg_primary_id = ? and boc.d_check_out_time is null "
                        + "order by boc.d_check_in_time desc limit 1",
                (resultSet, rowNum) -> resultSet.getString("v_bed_desc"),
                regId);
        return rows.isEmpty() ? "" : rows.get(0);
    }

    /** Saldo deposit = baris terakhir tb_patient_deposit. Migrasi getBalancet(). */
    public Double findDepositBalance(Integer regId) {
        List<Double> rows = jdbcTemplate.query(
                "select n_balance from tb_patient_deposit where n_reg_id = ? "
                        + "order by d_whn_create desc limit 1",
                (resultSet, rowNum) -> toDouble(resultSet.getObject("n_balance")),
                regId);
        return rows.isEmpty() ? 0.0 : rows.get(0);
    }

    // ------------------------------------------------------------------ nota

    /** Cari nota BELUM LUNAS (validated) untuk pembayaran. */
    public List<CashierNoteResponse> searchNotes(Integer unitId, String noteNo, String patientName) {
        StringBuilder sql = new StringBuilder();
        sql.append("select note.n_exam_id, note.v_note_no, note.n_exam_status, ")
                .append("note.n_total_amount, note.d_whn_create, pat.v_patient_name ")
                .append("from tb_examination note ")
                .append("join ms_patient pat on pat.n_patient_id = note.n_patient_id ")
                .append("where note.n_payment_status = ? and note.n_exam_status = ? ")
                .append("and note.v_note_no like ? and upper(pat.v_patient_name) like ? ");
        List<Object> params = new ArrayList<>();
        params.add((int) BELUM_LUNAS);
        params.add((int) NOTE_VALIDATED);
        params.add(like(normalizeOptionalUpper(noteNo)));
        params.add(like(normalizeOptionalUpper(patientName)));
        if (unitId != null) {
            sql.append("and note.n_unit_id = ? ");
            params.add(unitId);
        }
        sql.append("order by note.d_whn_create desc limit 100");
        return jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> new CashierNoteResponse(
                        resultSet.getInt("n_exam_id"),
                        resultSet.getString("v_note_no"),
                        resultSet.getString("v_patient_name"),
                        toIsoDateTime(resultSet.getTimestamp("d_whn_create")),
                        resultSet.getInt("n_exam_status"),
                        "SUDAH VALIDASI",
                        toDouble(resultSet.getObject("n_total_amount"))));
    }

    /** Baris nota untuk tabel transaksi kasir. */
    public List<CashierNoteLineResponse> getNoteLines(Integer noteId) {
        List<CashierNoteLineResponse> lines = new ArrayList<>();
        lines.addAll(jdbcTemplate.query(
                "select trx.n_note_id, treat.v_treatment_code as v_item_code, "
                        + "treat.v_treatment_name as v_item_name, trx.n_qty, "
                        + "trx.n_amount_trx, trx.n_disc_amount, trx.n_amount_after_disc, "
                        + "trx.v_disc_type, '-' as v_mitem_end_quantify "
                        + "from tb_treatment_trx trx "
                        + "join ms_treatment_fee tfee on tfee.n_treatment_fee_id = trx.n_treatment_fee_id "
                        + "join ms_treatment treat on treat.n_treatment_id = tfee.n_treatment_id "
                        + "where trx.n_note_id = ?",
                (resultSet, rowNum) -> mapLine(resultSet),
                noteId));
        lines.addAll(jdbcTemplate.query(
                "select trx.n_note_id, item.v_item_code, item.v_item_name, trx.n_qty, "
                        + "trx.n_amount_trx, trx.n_disc_amount, trx.n_amount_after_disc, "
                        + "trx.v_disc_type, meas.v_mitem_end_quantify "
                        + "from tb_item_trx trx "
                        + "join ms_item item on item.n_item_id = trx.n_item_id "
                        + "join ms_item_measurement meas on meas.n_mitem_id = item.n_mitem_id "
                        + "where trx.n_note_id = ?",
                (resultSet, rowNum) -> mapLine(resultSet),
                noteId));
        lines.addAll(jdbcTemplate.query(
                "select n_note_id, v_misc_name, n_qty, n_amount_trx, n_disc_amount, "
                        + "n_amount_after_disc, v_disc_type "
                        + "from tb_misc_trx where n_note_id = ?",
                (resultSet, rowNum) -> {
                    double qty = nvlDouble(resultSet.getObject("n_qty"), 1);
                    double amount = nvlDouble(resultSet.getObject("n_amount_trx"), 0);
                    return new CashierNoteLineResponse(
                            resultSet.getInt("n_note_id"),
                            null,
                            "MISC-001",
                            resultSet.getString("v_misc_name"),
                            qty, "-",
                            qty == 0 ? 0 : amount / qty,
                            nvlDouble(resultSet.getObject("n_disc_amount"), 0),
                            nvlDouble(resultSet.getObject("n_amount_after_disc"), 0));
                },
                noteId));
        return lines;
    }

    private CashierNoteLineResponse mapLine(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        double qty = nvlDouble(resultSet.getObject("n_qty"), 1);
        double amount = nvlDouble(resultSet.getObject("n_amount_trx"), 0);
        String unit = "";
        try {
            unit = resultSet.getString("v_mitem_end_quantify");
        } catch (java.sql.SQLException ignored) {
            unit = "-";
        }
        if (unit == null) {
            unit = "-";
        }
        return new CashierNoteLineResponse(
                resultSet.getInt("n_note_id"),
                null,
                resultSet.getString("v_item_code") != null ? resultSet.getString("v_item_code")
                        : resultSet.getString("v_treatment_code"),
                resultSet.getString("v_item_name") != null ? resultSet.getString("v_item_name")
                        : resultSet.getString("v_treatment_name"),
                qty, unit,
                qty == 0 ? 0 : amount / qty,
                nvlDouble(resultSet.getObject("n_disc_amount"), 0),
                nvlDouble(resultSet.getObject("n_amount_after_disc"), 0));
    }

    // ------------------------------------------------------------------ bayar

    /**
     * Pelunasan nota. Migrasi dari {@code CashierController.doSave()} +
     * {@code CashierDAO.saveBillNotas()}: buat kwitansi (KPJ-), settlement,
     * tandai nota LUNAS, kurangi saldo deposit.
     */
    @Transactional
    public CashierPayResultResponse pay(CashierPayRequest request, String username) {
        if (request.getNoteIds() == null || request.getNoteIds().isEmpty()) {
            throw new IllegalArgumentException("PILIH NOTA TERLEBIH DAHULU!");
        }
        if (request.getRegistrationId() == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        double total = 0;
        for (Integer noteId : request.getNoteIds()) {
            Double noteTotal = findNoteTotal(noteId);
            total += noteTotal == null ? 0 : noteTotal;
        }

        double discount = request.getDiscount() == null ? 0 : request.getDiscount();
        double discountValue = "%".equals(request.getDiscountType())
                ? total * discount / 100.0 : discount;
        double ppn = request.getPpn() == null ? 0 : request.getPpn();
        double base = total - discountValue;
        double tax = ppn > 0 ? base * ppn / 100.0 : 0;
        double totalAmount = base + tax;

        String kwitansiCode = generateBillCode(now);
        jdbcTemplate.update(
                "insert into tb_patient_bill (n_pbill_id, n_reg_id, v_pbill_code, v_name_on_bill, "
                        + "v_addr_on_bill, n_pbill_sub_ttl, n_pbill_ttl_paid, n_pbill_disc, "
                        + "n_pbill_tax, v_who_create, d_whn_create, d_settlement_date, n_payment_status) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getNextSequence("tb_patient_bill_n_pbill_id_seq"),
                request.getRegistrationId(), kwitansiCode,
                request.getNameOnBill(), request.getAddrOnBill(),
                total, totalAmount, (float) discountValue, (float) tax,
                username, now, now, (int) BELUM_LUNAS);
        Integer billId = jdbcTemplate.queryForObject(
                "select max(n_pbill_id) from tb_patient_bill where v_pbill_code = ?",
                Integer.class, kwitansiCode);

        double depositPaid = request.getDeposit() == null ? 0 : request.getDeposit();
        double cash = request.getCash() == null ? 0 : request.getCash();

        // settlement tunai + deposit
        if (cash > 0) {
            insertSettlement(billId, CASH_SETTLEMENT, Math.min(cash, totalAmount - depositPaid),
                    null, null, null, username, now);
        }
        if (depositPaid > 0) {
            insertSettlement(billId, DEPOSIT_SETTLEMENT, depositPaid, null, null, null, username, now);
            applyDepositMutation(request.getRegistrationId(), -depositPaid, username, now);
        }
        // settlement bank / asuransi dari tab CARA PEMBAYARAN
        if (request.getSettlements() != null) {
            for (CashierSettlementRequest settlement : request.getSettlements()) {
                if (settlement.getAmount() == null || settlement.getAmount() <= 0) {
                    continue;
                }
                if (settlement.getType() != null && settlement.getType() == BANK_SETTLEMENT) {
                    insertSettlement(billId, BANK_SETTLEMENT, settlement.getAmount(),
                            settlement.getBankId(), null, settlement.getAccountNo(), username, now);
                } else if (settlement.getType() != null && settlement.getType() == INSURANCE_SETTLEMENT) {
                    insertSettlement(billId, INSURANCE_SETTLEMENT, settlement.getAmount(),
                            null, settlement.getInsuranceId(), null, username, now);
                }
            }
        }

        // tandai nota LUNAS + ikat ke kwitansi
        for (Integer noteId : request.getNoteIds()) {
            jdbcTemplate.update(
                    "update tb_examination set n_payment_status = ?, n_pbill_id = ?, "
                            + "d_whn_change = ?, v_who_change = ? where n_exam_id = ?",
                    (int) SUDAH_LUNAS, billId, now, username, noteId);
        }

        createPaymentJournal(billId, kwitansiCode, totalAmount, request.getUnitId(), username, now);

        return new CashierPayResultResponse(true, "Pembayaran berhasil disimpan.",
                kwitansiCode, findDepositBalance(request.getRegistrationId()));
    }

    /**
     * Deposit / retur deposit pasien rawat inap. Migrasi dari
     * {@code CashierTransactionController.doSave()} (DEPOSIT / RETUR-DEPOSIT).
     */
    @Transactional
    public CashierPayResultResponse deposit(CashierDepositRequest request, boolean retur,
            String username) {
        if (request.getRegistrationId() == null) {
            throw new IllegalArgumentException("PILIH PASIEN TERLEBIH DAHULU!");
        }
        if (request.getAmount() == null || request.getAmount() == 0) {
            throw new IllegalArgumentException("JUMLAH DEPOSIT HARUS DI ISI!");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        double mutation = retur ? -request.getAmount() : request.getAmount();
        double balance = findDepositBalance(request.getRegistrationId()) + mutation;
        if (balance < 0) {
            throw new IllegalArgumentException("SALDO DEPOSIT TIDAK CUKUP!");
        }
        applyDepositMutation(request.getRegistrationId(), mutation, username, now);

        String kwitansiCode = generateBillCode(now);
        jdbcTemplate.update(
                "insert into tb_patient_bill (n_pbill_id, n_reg_id, v_pbill_code, v_name_on_bill, "
                        + "v_addr_on_bill, n_pbill_ttl_paid, v_who_create, d_whn_create, "
                        + "d_settlement_date, n_payment_status) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getNextSequence("tb_patient_bill_n_pbill_id_seq"),
                request.getRegistrationId(), kwitansiCode,
                request.getNameOnBill(), request.getAddrOnBill(),
                retur ? -request.getAmount() : request.getAmount(),
                username, now, now, (int) BELUM_LUNAS);
        Integer billId = jdbcTemplate.queryForObject(
                "select max(n_pbill_id) from tb_patient_bill where v_pbill_code = ?",
                Integer.class, kwitansiCode);
        insertSettlement(billId, DEPOSIT_SETTLEMENT, request.getAmount(), null, null, null,
                username, now);

        createPaymentJournal(billId, kwitansiCode, request.getAmount(), request.getUnitId(),
                username, now);
        return new CashierPayResultResponse(true,
                retur ? "Retur deposit berhasil." : "Deposit berhasil disimpan.",
                kwitansiCode, balance);
    }

    private void applyDepositMutation(Integer regId, double mutation, String username, Timestamp now) {
        double balance = findDepositBalance(regId) + mutation;
        jdbcTemplate.update(
                "insert into tb_patient_deposit (n_pd_id, n_reg_id, n_mutation, n_balance, "
                        + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?)",
                getNextSequence("tb_patient_deposit_n_pd_id_seq"),
                regId, mutation, balance, username, now);
    }

    private void insertSettlement(Integer billId, short type, double amount, Integer bankId,
            Integer insuranceId, String accountNo, String username, Timestamp now) {
        jdbcTemplate.update(
                "insert into tb_patient_settlement (n_psettlement_id, n_pbill_id, n_amount_settled, "
                        + "n_psettlement_type, n_bank_debit, n_insurance_id, v_patient_account_no, "
                        + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getNextSequence("tb_patient_settlement_n_psettlement_id_seq"),
                billId, amount, (int) type, bankId, insuranceId, accountNo, username, now);
    }

    /**
     * Journal pembayaran: DR asset (kas unit / bank / asuransi / deposit) CR AR.
     * Migrasi inti dari {@code CashierDAO.saveBillNotas()}.
     */
    private void createPaymentJournal(Integer billId, String kwitansiCode, double totalAmount,
            Integer unitId, String username, Timestamp now) {
        if (totalAmount == 0) {
            return;
        }
        Integer coaArId = findCoaIdByGimKey("COA_OUTPATIENT_AR");
        if (coaArId == null) {
            coaArId = findCoaIdByGimKey("COA_INPATIENT_AR");
        }
        Integer coaKasId = unitId == null ? null : findUnitCoaId(unitId);
        String batchId = "AR" + String.format("%015d", getNextSequence("sq_journal_trx"));
        if (coaKasId != null && coaArId != null) {
            insertJournal(batchId, kwitansiCode, "PEMBAYARAN;KWITANSI:" + kwitansiCode,
                    totalAmount, 0, now, username, coaKasId);
            insertJournal(batchId, kwitansiCode, "PEMBAYARAN;KWITANSI:" + kwitansiCode,
                    0, totalAmount, now, username, coaArId);
        }
    }

    private void insertJournal(String batchId, String voucherNo, String desc, double debit,
            double credit, Timestamp now, String username, Integer coaId) {
        jdbcTemplate.update(
                "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, "
                        + "v_desc, n_debit, n_credit, d_whn_create, v_who_create, d_apl_date, n_coa_id) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getNextSequence("tb_journal_trx_n_journal_id_seq"),
                batchId, voucherNo, desc, debit, credit, now, username, now, coaId);
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

    private Integer findUnitCoaId(Integer unitId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select n_coa_id from ms_unit where n_unit_id = ?", Integer.class, unitId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ helpers

    private Double findNoteTotal(Integer noteId) {
        List<Double> rows = jdbcTemplate.query(
                "select n_total_amount from tb_examination where n_exam_id = ?",
                (resultSet, rowNum) -> toDouble(resultSet.getObject("n_total_amount")),
                noteId);
        return rows.isEmpty() ? 0.0 : rows.get(0);
    }

    private String generateBillCode(Timestamp now) {
        Integer sequence = getNextSequence("kwintansi_seq");
        return "KPJ-" + now.toLocalDateTime().format(BILL_DATE) + "-"
                + String.format("%05d", sequence);
    }

    private Integer getNextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMrCode(String mrCode) {
        return mrCode == null ? "" : mrCode.trim().toUpperCase(Locale.ROOT);
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

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class PatientRow {
        private final int mrId;
        private final String mrCode;
        private final String patientName;
        private final String address;
        private final String patientTypeName;

        private PatientRow(int mrId, String mrCode, String patientName, String address,
                String patientTypeName) {
            this.mrId = mrId;
            this.mrCode = mrCode;
            this.patientName = patientName;
            this.address = address;
            this.patientTypeName = patientTypeName;
        }
    }

    private static final class RegistrationRow {
        private final int regId;
        private final String regNo;

        private RegistrationRow(int regId, String regNo) {
            this.regId = regId;
            this.regNo = regNo;
        }
    }
}
