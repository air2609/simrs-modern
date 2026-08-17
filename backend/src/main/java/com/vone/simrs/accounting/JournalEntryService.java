package com.vone.simrs.accounting;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0199 (MANUAL JOURNAL ENTRY / journalEntry.zul).
 *
 * <p>
 * Migrasi dari legacy {@code JournalEntryController} + {@code JournalBeanHandler}
 * + {@code JournalTrxDAO.saveJournal()}:
 * <ul>
 * <li>{@code JournalEntryController.init()} → {@link #getMasters()}</li>
 * <li>{@code JournalEntryController.saveClick()} + {@code JournalBeanHandler.addJournal()}
 * → {@link #save(JournalEntrySaveRequest, String)}</li>
 * <li>{@code JournalEntryController.printToPdf()} → {@link #getPrintData(String)}</li>
 * </ul>
 */
@Service
public class JournalEntryService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SAVE_FAILURE = "TRANSAKSI GAGAL DISIMPAN";
    private static final String UNBALANCE_TRANSACTION = "TRANSAKSI TIDAK BALANCE";
    private static final String BATCH_TYPE = "GJ";

    private final JdbcTemplate jdbcTemplate;

    public JournalEntryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar opsi COA (COA_ALL) untuk dropdown ACCOUNT. Migrasi dari legacy
     * {@code CoaController.getCoaForSelect(coaList, COA_ALL)}.
     */
    public JournalEntryMastersResponse getMasters() {
        List<JournalEntryMastersResponse.CoaOption> options = jdbcTemplate.query(
                "select n_coa_id, v_acct_no, v_acct_name from ms_coa order by v_acct_no",
                (resultSet, rowNum) -> new JournalEntryMastersResponse.CoaOption(
                        resultSet.getInt("n_coa_id"),
                        resultSet.getString("v_acct_no"),
                        resultSet.getString("v_acct_name")));
        return new JournalEntryMastersResponse(options);
    }

    /**
     * Simpan jurnal manual. Migrasi dari legacy
     * {@code JournalEntryController.saveClick()}: setiap baris dengan debet
     * > 0 menjadi baris DEBET, selain itu baris KREDIT; cek balance sebelum
     * disimpan.
     *
     * @return pesan hasil (sukses / gagal / tidak balance)
     */
    @Transactional
    public String save(JournalEntrySaveRequest request, String username) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("ISILAH TRANSAKSINYA");
        }
        if (request.getVoucherNo() == null || request.getVoucherNo().trim().isEmpty()) {
            throw new IllegalArgumentException("VOUCHER NO. WAJIB DIISI!");
        }
        Timestamp aplDate = parseDate(request.getAplDate());
        if (aplDate == null) {
            throw new IllegalArgumentException("APL DATE WAJIB DIISI!");
        }

        String batchId = buildBatchId();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String actor = normalize(username);
        String voucherNo = request.getVoucherNo().trim().toUpperCase(Locale.ROOT);
        String description = request.getDescription() == null ? ""
                : request.getDescription().trim().toUpperCase(Locale.ROOT);

        double sumDebit = 0;
        double sumCredit = 0;
        for (JournalEntrySaveRequest.Line line : request.getLines()) {
            if (line.getCoaId() == null) {
                continue;
            }
            double debit = valueOrZero(line.getDebit());
            double credit = valueOrZero(line.getCredit());
            if (debit > 0) {
                insertLine(batchId, voucherNo, description, debit, 0, aplDate, now, actor,
                        line.getCoaId());
                sumDebit += debit;
            } else if (credit > 0) {
                insertLine(batchId, voucherNo, description, 0, credit, aplDate, now, actor,
                        line.getCoaId());
                sumCredit += credit;
            }
        }

        if (!isBalance(sumDebit, sumCredit)) {
            throw new IllegalArgumentException(UNBALANCE_TRANSACTION);
        }
        return "TRANSAKSI TELAH BERHASIL DISIMPAN";
    }

    /**
     * Data cetak manual journal per voucher. Migrasi dari legacy
     * {@code JournalEntryController.printToPdf()} (query + report
     * manual_jurnal.jrxml).
     */
    public JournalEntryPrintData getPrintData(String voucherNo) {
        if (voucherNo == null || voucherNo.trim().isEmpty()) {
            throw new IllegalArgumentException("VOUCHER NO. WAJIB DIISI!");
        }
        String voucher = voucherNo.trim().toUpperCase(Locale.ROOT);
        List<JournalEntryPrintData.Line> lines = jdbcTemplate.query(
                "select j.v_desc, c.v_acct_name || '[' || c.v_acct_no || ']' as account, "
                        + "coalesce(j.n_debit, 0) as n_debit, coalesce(j.n_credit, 0) as n_credit "
                        + "from tb_journal_trx j "
                        + "left join ms_coa c on c.n_coa_id = j.n_coa_id "
                        + "where j.v_voucher_no = ? order by j.n_journal_id",
                (resultSet, rowNum) -> new JournalEntryPrintData.Line(
                        resultSet.getString("v_desc"),
                        resultSet.getString("account"),
                        resultSet.getDouble("n_debit"),
                        resultSet.getDouble("n_credit")),
                voucher);

        String inputBy = "";
        String inputDate = "";
        try {
            java.util.Map<String, Object> header = jdbcTemplate.queryForList(
                    "select v_who_create, d_whn_create from tb_journal_trx "
                            + "where v_voucher_no = ? order by n_journal_id limit 1",
                    voucher).get(0);
            inputBy = header.get("v_who_create") == null ? ""
                    : header.get("v_who_create").toString();
            Object apl = header.get("d_whn_create");
            if (apl instanceof java.sql.Timestamp) {
                inputDate = new SimpleDateFormat("dd/MM/yyyy").format((java.sql.Timestamp) apl);
            }
        } catch (Exception ignored) {
            // header kosong
        }

        return new JournalEntryPrintData(voucher, inputBy, inputDate, lines);
    }

    private void insertLine(String batchId, String voucherNo, String description, double debit,
            double credit, Timestamp aplDate, Timestamp now, String actor, Integer coaId) {
        Integer journalId = nextVal("tb_journal_trx_n_journal_id_seq");
        jdbcTemplate.update(
                "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, "
                        + "v_desc, n_debit, n_credit, d_whn_create, v_who_create, d_apl_date, n_coa_id) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                journalId, batchId, voucherNo, description, debit, credit, now, actor, aplDate,
                coaId);
    }

    /**
     * Batch id "GJ" + 15 digit sequence. Migrasi dari legacy
     * {@code JournalBeanHandler.createJournalBatchId()}.
     */
    private String buildBatchId() {
        Integer seq = nextVal("sq_journal_trx");
        return BATCH_TYPE + String.format("%015d", seq);
    }

    /**
     * Cek balance debit vs kredit (dibulatkan 2 desimal). Migrasi dari legacy
     * {@code JournalBeanHandler.isBalance()}.
     */
    private boolean isBalance(double debit, double credit) {
        double debitRounded = Math.round(debit * 100) / 100.0;
        double creditRounded = Math.round(credit * 100) / 100.0;
        return Double.compare(debitRounded, creditRounded) == 0;
    }

    private Timestamp parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim(), ISO);
            return Timestamp.valueOf(date.atStartOfDay());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private Integer nextVal(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
