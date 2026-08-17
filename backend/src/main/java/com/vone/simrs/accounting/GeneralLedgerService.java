package com.vone.simrs.accounting;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0198 (GENERAL LEDGER / generalLedger.zul).
 *
 * <p>
 * Migrasi dari legacy {@code GeneralLedgerController} + {@code JournalTrxDAO}:
 * <ul>
 * <li>{@code getGLAll()} + {@code getGeneralLedgerAll()} →
 * {@link #getReport(String, String, Integer)} (coaId null)</li>
 * <li>{@code coaClick()} + {@code getGeneralLedgerByRange()} →
 * {@link #getReport(String, String, Integer)} (coaId terisi)</li>
 * <li>{@code cetakClick()} → {@link #getPrintData(Integer, String, String)}</li>
 * <li>{@code cetakAllClick()} → {@link #getPrintAllData()}</li>
 * </ul>
 */
@Service
public class GeneralLedgerService {

    private static final String DATE_FORMAT = "dd-MM-yyyy";

    private final JdbcTemplate jdbcTemplate;

    public GeneralLedgerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Data GL per periode. Jika {@code coaId} null → seluruh akun
     * ({@code func_gl_all_bydate}); jika terisi → satu akun
     * ({@code func_gl_bydate_arif}).
     */
    public List<GeneralLedgerRowResponse> getReport(String from, String to, Integer coaId) {
        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        String sql;
        Object[] params;
        if (coaId == null) {
            sql = "select n_row, d_apl_date, v_journal_batch_id, v_voucher_no, v_desc, "
                    + "n_debit, n_credit, n_balance, v_acct_name "
                    + "from report.func_gl_all_bydate(to_date(?, 'yyyy-MM-dd'), to_date(?, 'yyyy-MM-dd'))";
            params = new Object[] { from.trim(), to.trim() };
        } else {
            sql = "select n_row, d_apl_date, v_journal_batch_id, v_voucher_no, v_desc, "
                    + "n_debit, n_credit, n_balance, v_acct_name "
                    + "from report.func_gl_bydate_arif(?, to_date(?, 'yyyy-MM-dd'), to_date(?, 'yyyy-MM-dd'))";
            params = new Object[] { coaId, from.trim(), to.trim() };
        }
        return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), params);
    }

    /**
     * Data cetak GL satu akun (PRINT). Migrasi dari legacy
     * {@code cetakClick()} yang memakai {@code report.func_gl_bydate(...)}
     * dengan parameter "DATE RANGE: ... S/D ...".
     */
    public GeneralLedgerPrintData getPrintData(Integer coaId, String from, String to) {
        if (coaId == null) {
            throw new IllegalArgumentException("PILIH ACCOUNT TERLEBIH DAHULU!");
        }
        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        List<GeneralLedgerPrintData.Line> lines = jdbcTemplate.query(
                "select n_row, d_apl_date, v_journal_batch_id, v_voucher_no, v_desc, "
                        + "n_debit, n_credit, n_balance, v_acct_name "
                        + "from report.func_gl_bydate(?, to_date(?, 'MM/dd/yyyy'), to_date(?, 'MM/dd/yyyy'))",
                (resultSet, rowNum) -> mapPrintLine(resultSet),
                coaId, toUsDate(from), toUsDate(to));
        String dateParam = "DATE RANGE: " + displayDate(from) + " S/D " + displayDate(to);
        return new GeneralLedgerPrintData(dateParam, lines);
    }

    /**
     * Data cetak GL seluruh akun (PRINT ALL). Migrasi dari legacy
     * {@code cetakAllClick()} yang memakai {@code report.func_gl_all()}.
     */
    public GeneralLedgerPrintData getPrintAllData() {
        List<GeneralLedgerPrintData.Line> lines = jdbcTemplate.query(
                "select n_row, d_apl_date, v_journal_batch_id, v_voucher_no, v_desc, "
                        + "n_debit, n_credit, n_balance, v_acct_name from report.func_gl_all()",
                (resultSet, rowNum) -> mapPrintLine(resultSet));
        return new GeneralLedgerPrintData("", lines);
    }

    private GeneralLedgerRowResponse mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        String[] account = parseAccount(resultSet.getString("v_acct_name"));
        return new GeneralLedgerRowResponse(
                account[1],
                account[0],
                resultSet.getString("v_journal_batch_id"),
                resultSet.getString("v_voucher_no"),
                resultSet.getString("v_desc"),
                toDisplayDate(resultSet.getTimestamp("d_apl_date")),
                toDouble(resultSet.getObject("n_debit")),
                toDouble(resultSet.getObject("n_credit")),
                toDouble(resultSet.getObject("n_balance")));
    }

    private GeneralLedgerPrintData.Line mapPrintLine(java.sql.ResultSet resultSet)
            throws java.sql.SQLException {
        return new GeneralLedgerPrintData.Line(
                resultSet.getString("v_acct_name"),
                resultSet.getObject("n_row") == null ? null : resultSet.getLong("n_row"),
                resultSet.getString("v_journal_batch_id"),
                resultSet.getString("v_voucher_no"),
                resultSet.getString("v_desc"),
                toDisplayDate(resultSet.getTimestamp("d_apl_date")),
                toDouble(resultSet.getObject("n_debit")),
                toDouble(resultSet.getObject("n_credit")),
                toDouble(resultSet.getObject("n_balance")));
    }

    /**
     * Parse "NAMA[NO]" menjadi [nama, no]. Migrasi dari legacy
     * {@code GeneralLedgerController.getAccount()} (dengan trim spasi).
     */
    private String[] parseAccount(String value) {
        String name = value == null ? "" : value.trim();
        String no = "";
        if (value != null) {
            String normalized = value.replace("[", "&").replace("]", "");
            String[] parts = normalized.split("&");
            if (parts.length > 1) {
                name = parts[0].trim();
                no = parts[1].trim();
            } else {
                name = parts[0].trim();
            }
        }
        return new String[] { name, no };
    }

    private String toUsDate(String isoDate) {
        String[] parts = isoDate.split("-");
        return parts.length == 3 ? parts[1] + "/" + parts[2] + "/" + parts[0] : isoDate;
    }

    private String displayDate(String isoDate) {
        String[] parts = isoDate.split("-");
        return parts.length == 3 ? parts[2] + "/" + parts[1] + "/" + parts[0] : isoDate;
    }

    private String toDisplayDate(Timestamp value) {
        return value == null ? "" : new SimpleDateFormat(DATE_FORMAT).format(value);
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
}
