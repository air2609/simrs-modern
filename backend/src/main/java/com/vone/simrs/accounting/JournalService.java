package com.vone.simrs.accounting;

import com.vone.simrs.auth.AuthenticationRequiredException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class JournalService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;

    public JournalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String requireUsername(HttpSession session) {
        if (session == null) {
            throw new AuthenticationRequiredException("Your session has been expired. You need to login again.");
        }

        Object username = session.getAttribute("USER_INFO");
        if (!(username instanceof String)) {
            throw new AuthenticationRequiredException("Your session has been expired. You need to login again.");
        }

        return (String) username;
    }

    public List<JournalEntryResponse> searchJournals(String voucherNo, String dateFrom, String dateTo) {
        StringBuilder sql = new StringBuilder();
        sql.append("select j.n_journal_id, j.v_journal_batch_id, j.v_voucher_no, j.v_desc, ");
        sql.append("  j.n_debit, j.n_credit, j.d_apl_date, j.n_coa_id, ");
        sql.append("  c.v_acct_no, c.v_acct_name ");
        sql.append("from tb_journal_trx j ");
        sql.append("left join ms_coa c on c.n_coa_id = j.n_coa_id ");
        sql.append("where j.v_voucher_no like ? ");
        sql.append("  and j.d_apl_date >= CAST(? AS date) ");
        sql.append("  and j.d_apl_date < CAST(? AS date) ");
        sql.append("order by j.d_apl_date desc, j.n_journal_id");

        String voucherPattern = "%" + (voucherNo != null ? voucherNo.trim() : "") + "%";
        String fromDate = dateFrom != null && !dateFrom.trim().isEmpty() ? dateFrom.trim() : "1970-01-01";
        String toDate = dateTo != null && !dateTo.trim().isEmpty() ? dateTo.trim() : "2099-12-31";

        // Add 1 day to toDate (matching legacy behavior) so the range
        // covers all timestamps on the target date using < (exclusive).
        if (!"2099-12-31".equals(toDate)) {
            LocalDate parsed = LocalDate.parse(toDate);
            toDate = parsed.plusDays(1).toString();
        }

        return jdbcTemplate.query(sql.toString(), new JournalRowMapper(), voucherPattern, fromDate, toDate);
    }

    /** Shared: insert a single journal entry row */
    public void insertJournalEntry(String batchId, String voucherNo, String description,
            double debit, double credit, Timestamp now, String username, Integer coaId) {
        Integer journalId = nextSequence("tb_journal_trx_n_journal_id_seq");
        jdbcTemplate.update(
            "insert into tb_journal_trx (n_journal_id, v_journal_batch_id, v_voucher_no, "
                + "v_desc, n_debit, n_credit, d_whn_create, v_who_create, d_apl_date, n_coa_id) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            journalId, batchId, voucherNo, description, debit, credit, now, username, now, coaId);
    }

    /** Shared: build journal batch id (AR + sequence) */
    public String buildJournalBatchId() {
        Integer sequence = nextSequence("sq_journal_trx");
        return "AR" + String.format("%015d", sequence);
    }

    /** Shared: look up COA id by gim_key from ms_gim -> ms_coa */
    public Integer findCoaIdByGimKey(String gimKey) {
        try {
            return jdbcTemplate.queryForObject(
                "select coa.n_coa_id from ms_gim gim "
                    + "join ms_coa coa on coa.v_acct_no = gim.v_value "
                    + "where gim.v_key = ?",
                Integer.class, gimKey);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /** Shared: get next value from a sequence */
    public Integer nextSequence(String sequenceName) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Integer.class);
    }

    private static class JournalRowMapper implements RowMapper<JournalEntryResponse> {
        @Override
        public JournalEntryResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp aplDateTs = rs.getTimestamp("d_apl_date");
            String formattedDate = aplDateTs != null
                    ? aplDateTs.toLocalDateTime().toLocalDate().format(DATE_FORMATTER)
                    : "";

            Double debit = rs.getObject("n_debit") != null ? rs.getDouble("n_debit") : 0.0;
            Double credit = rs.getObject("n_credit") != null ? rs.getDouble("n_credit") : 0.0;

            return new JournalEntryResponse(
                    rs.getInt("n_journal_id"),
                    rs.getString("v_journal_batch_id"),
                    rs.getString("v_voucher_no"),
                    rs.getString("v_desc"),
                    debit,
                    credit,
                    formattedDate,
                    rs.getObject("n_coa_id") != null ? rs.getInt("n_coa_id") : null,
                    rs.getString("v_acct_no"),
                    rs.getString("v_acct_name")
            );
        }
    }
}
