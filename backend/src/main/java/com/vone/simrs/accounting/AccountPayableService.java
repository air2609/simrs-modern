package com.vone.simrs.accounting;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service untuk screen SC0196 (ACCOUNT PAYABLE / apScreen.zul).
 *
 * <p>
 * Migrasi dari legacy {@code AccountPayableController} + {@code JournalManagerImpl}
 * + {@code JournalTrxDAO}:
 * <ul>
 * <li>{@code AccountPayableController.getAllAp()} +
 * {@code JournalTrxDAO.getAllAp()} → {@link #getAllAp()}</li>
 * <li>{@code lihatJurnalClick()} + {@code getJournalByBatch()} →
 * {@link #getJournalByBatch(String)}</li>
 * <li>{@code historyClick()} + {@code getJournalByApId()} →
 * {@link #getPaymentHistory(Integer)}</li>
 * <li>{@code pembayaranClick()} + {@code savePayment()} →
 * {@link #pay(AccountPayablePayRequest, String)}</li>
 * </ul>
 */
@Service
public class AccountPayableService {

    private static final String SAVE_FAILURE = "TRANSAKSI GAGAL DISIMPAN";
    private static final String PEMBAYARAN_MELEBIHI_HUTANG = "PEMBAYARAN TDK BOLEH MELEBIHI HUTANG";
    private static final String ISILAH_JUMLAH_PEMBAYARAN = "ISILAH JUMLAH PEMBAYARAN DENGAN BAIK DAN BENAR";
    private static final String ISILAH_REKENING = "ISILAH REKENING PEMBAYAR DENGAN BAIK DAN BENAR";
    private static final String PEMBAYARAN_POSITIP = "PEMBAYARAN HARUS POSITIP";
    private static final String BATCH_TYPE = "PM";

    private final JdbcTemplate jdbcTemplate;

    public AccountPayableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Daftar AP dengan sisa hutang &gt; 0, didukung pencarian (nama supplier /
     * journal batch id) dan paging (default 20 baris). Migrasi dari legacy
     * {@code JournalTrxDAO.getAllAp()} + paging legacy pageSize 28.
     */
    public AccountPayablePageResponse getAllAp(String keyword, Integer page, Integer pageSize) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 20 : pageSize;
        String pattern = "%" + normalize(keyword) + "%";

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from tb_account_payable ap "
                        + "left join ms_vendor v on v.n_vendor_id = ap.n_vendor_id "
                        + "left join tb_journal_trx j on j.n_journal_id = ap.n_journal_id "
                        + "where ap.n_total_remaining > 0 "
                        + "and (upper(coalesce(v.v_vendor_name, '')) like ? "
                        + "or upper(coalesce(j.v_journal_batch_id, '')) like ?)",
                Long.class, pattern, pattern);

        List<AccountPayableRowResponse> rows = jdbcTemplate.query(
                "select ap.n_ap_id, coalesce(v.v_vendor_name, '-') as v_vendor_name, "
                        + "j.v_journal_batch_id, coalesce(ap.n_total_remaining, 0) as n_total_remaining, "
                        + "ap.d_due_date "
                        + "from tb_account_payable ap "
                        + "left join ms_vendor v on v.n_vendor_id = ap.n_vendor_id "
                        + "left join tb_journal_trx j on j.n_journal_id = ap.n_journal_id "
                        + "where ap.n_total_remaining > 0 "
                        + "and (upper(coalesce(v.v_vendor_name, '')) like ? "
                        + "or upper(coalesce(j.v_journal_batch_id, '')) like ?) "
                        + "order by v.v_vendor_name, ap.n_ap_id "
                        + "limit ? offset ?",
                (resultSet, rowNum) -> new AccountPayableRowResponse(
                        resultSet.getInt("n_ap_id"),
                        resultSet.getString("v_vendor_name"),
                        resultSet.getString("v_journal_batch_id") == null ? "-"
                                : resultSet.getString("v_journal_batch_id"),
                        resultSet.getDouble("n_total_remaining"),
                        toDisplayDate(resultSet.getTimestamp("d_due_date"))),
                pattern, pattern, size, (currentPage - 1) * size);

        long totalCount = total == null ? 0 : total;
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalCount / size);
        return new AccountPayablePageResponse(rows, totalCount, currentPage, size, totalPages);
    }

    /**
     * Opsi COA untuk dropdown VIA ACCT pada dialog pembayaran.
     */
    public AccountPayableMastersResponse getMasters() {
        List<JournalEntryMastersResponse.CoaOption> options = jdbcTemplate.query(
                "select n_coa_id, v_acct_no, v_acct_name from ms_coa order by v_acct_no",
                (resultSet, rowNum) -> new JournalEntryMastersResponse.CoaOption(
                        resultSet.getInt("n_coa_id"),
                        resultSet.getString("v_acct_no"),
                        resultSet.getString("v_acct_name")));
        return new AccountPayableMastersResponse(options);
    }

    /**
     * Detail jurnal per batch id (dialog LIHAT JOURNAL). Migrasi dari legacy
     * {@code JournalManagerImpl.getJournalByBatch()}.
     */
    public List<AccountPayableJournalResponse> getJournalByBatch(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("BATCH ID WAJIB DIISI!");
        }
        return jdbcTemplate.query(
                "select j.v_journal_batch_id, j.v_voucher_no, "
                        + "coalesce(c.v_acct_name, '') as v_acct_name, "
                        + "coalesce(j.v_desc, '') as v_desc, "
                        + "coalesce(j.n_debit, 0) as n_debit, coalesce(j.n_credit, 0) as n_credit, "
                        + "j.d_apl_date "
                        + "from tb_journal_trx j "
                        + "left join ms_coa c on c.n_coa_id = j.n_coa_id "
                        + "where j.v_journal_batch_id = ? order by j.n_journal_id",
                (resultSet, rowNum) -> mapJournalRow(resultSet),
                batchId.trim());
    }

    /**
     * History pembayaran per AP (dialog LIHAT HISTORY PEMBAYARAN). Migrasi
     * dari legacy {@code JournalManagerImpl.getJournalByApId()}.
     */
    public List<AccountPayableJournalResponse> getPaymentHistory(Integer apId) {
        if (apId == null) {
            throw new IllegalArgumentException("AP TIDAK DIPILIH!");
        }
        return jdbcTemplate.query(
                "select j.v_journal_batch_id, j.v_voucher_no, "
                        + "coalesce(c.v_acct_name, '') as v_acct_name, "
                        + "coalesce(j.v_desc, '') as v_desc, "
                        + "coalesce(j.n_debit, 0) as n_debit, coalesce(j.n_credit, 0) as n_credit, "
                        + "j.d_apl_date "
                        + "from tb_journal_trx j "
                        + "join tb_account_payable_detail apd on apd.v_journal_batch_id = j.v_journal_batch_id "
                        + "left join ms_coa c on c.n_coa_id = j.n_coa_id "
                        + "where apd.n_ap_id = ? order by j.n_journal_id",
                (resultSet, rowNum) -> mapJournalRow(resultSet),
                apId);
    }

    /**
     * Proses pembayaran hutang. Migrasi dari legacy
     * {@code AccountPayableController.pembayaranClick()} +
     * {@code JournalTrxDAO.savePayment()}.
     *
     * @return pesan hasil
     */
    @Transactional
    public String pay(AccountPayablePayRequest request, String username) {
        if (request.getApId() == null) {
            throw new IllegalArgumentException(SAVE_FAILURE);
        }
        if (request.getTotal() == null) {
            throw new IllegalArgumentException(ISILAH_JUMLAH_PEMBAYARAN);
        }
        if (request.getViaCoaId() == null) {
            throw new IllegalArgumentException(ISILAH_REKENING);
        }
        double totalBayar = request.getTotal();
        if (totalBayar <= 0) {
            throw new IllegalArgumentException(PEMBAYARAN_POSITIP);
        }

        // Ambil AP + sisa hutang + COA jurnal AP
        ApHeader ap = loadAp(request.getApId());
        if (ap == null) {
            throw new IllegalArgumentException(SAVE_FAILURE);
        }
        if (ap.totalRemaining < totalBayar) {
            throw new IllegalArgumentException(PEMBAYARAN_MELEBIHI_HUTANG);
        }
        if (ap.apCoaId == null) {
            throw new IllegalArgumentException("JURNAL AP TIDAK DITEMUKAN (COA DEBIT TIDAK TERSEDIA).");
        }

        String batchId = buildBatchId();
        String voucherNo = generateVoucherNo();
        String memo = request.getMemo() == null ? "" : request.getMemo().trim();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String actor = normalize(username);

        // Jurnal: CREDIT ke rekening pembayar (via), DEBIT ke COA jurnal AP
        insertJournal(batchId, voucherNo, memo, 0, totalBayar, now, actor, request.getViaCoaId());
        insertJournal(batchId, voucherNo, memo, totalBayar, 0, now, actor, ap.apCoaId);

        // Update sisa hutang
        jdbcTemplate.update(
                "update tb_account_payable set n_total_remaining = ?, v_who_change = ?, "
                        + "d_whn_change = ? where n_ap_id = ?",
                ap.totalRemaining - totalBayar, actor, now, request.getApId());

        // Catat detail pembayaran
        Integer apdId = nextVal("tb_account_payable_detail_n_apd_id_seq");
        jdbcTemplate.update(
                "insert into tb_account_payable_detail (n_apd_id, n_ap_id, v_journal_batch_id, "
                        + "v_who_create, d_whn_create) values (?, ?, ?, ?, ?)",
                apdId, request.getApId(), batchId, actor, now);

        return "PEMBAYARAN BERHASIL";
    }

    private void insertJournal(String batchId, String voucherNo, String memo, double debit,
            double credit, Timestamp now, String actor, Integer coaId) {
        Integer journalId = nextVal("tb_journal_trx_n_journal_id_seq");
        jdbcTemplate.update(
                "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, "
                        + "v_desc, n_debit, n_credit, d_whn_create, v_who_create, d_apl_date, n_coa_id) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                journalId, batchId, voucherNo, memo, debit, credit, now, actor, now, coaId);
    }

    private AccountPayableJournalResponse mapJournalRow(
            java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new AccountPayableJournalResponse(
                resultSet.getString("v_journal_batch_id"),
                resultSet.getString("v_voucher_no"),
                resultSet.getString("v_acct_name"),
                resultSet.getString("v_desc"),
                resultSet.getDouble("n_debit"),
                resultSet.getDouble("n_credit"),
                toDisplayDate(resultSet.getTimestamp("d_apl_date")));
    }

    private ApHeader loadAp(Integer apId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select ap.n_ap_id, coalesce(ap.n_total_remaining, 0) as n_total_remaining, "
                            + "j.n_coa_id as ap_coa_id "
                            + "from tb_account_payable ap "
                            + "left join tb_journal_trx j on j.n_journal_id = ap.n_journal_id "
                            + "where ap.n_ap_id = ?",
                    (resultSet, rowNum) -> new ApHeader(
                            resultSet.getInt("n_ap_id"),
                            resultSet.getDouble("n_total_remaining"),
                            toInteger(resultSet.getObject("ap_coa_id"))),
                    apId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    /**
     * Batch id "PM" + 15 digit sequence. Migrasi dari legacy
     * {@code JournalBeanHandler.createJournalBatchId()}.
     */
    private String buildBatchId() {
        Integer seq = nextVal("sq_journal_trx");
        return BATCH_TYPE + String.format("%015d", seq);
    }

    /**
     * Voucher no "AP-PAYMENT-XXXXXX". Migrasi dari legacy
     * {@code MedisafeUtil.generateVoucherNo()} + {@code convertToNotaNumber()}.
     */
    private String generateVoucherNo() {
        Integer seq = nextVal("voucher_no_seq");
        return "AP-PAYMENT-" + String.format("%06d", seq);
    }

    private Integer nextVal(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String toDisplayDate(Timestamp value) {
        return value == null ? "-" : new SimpleDateFormat("dd-MM-yyyy").format(value);
    }

    private String normalize(String value) {
        return value == null ? "SYSTEM" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static class ApHeader {

        final Integer apId;
        final double totalRemaining;
        final Integer apCoaId;

        ApHeader(Integer apId, double totalRemaining, Integer apCoaId) {
            this.apId = apId;
            this.totalRemaining = totalRemaining;
            this.apCoaId = apCoaId;
        }
    }
}
