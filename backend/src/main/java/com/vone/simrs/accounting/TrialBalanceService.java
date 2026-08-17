package com.vone.simrs.accounting;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0207 (TRIAL BALANCE / trialBalance.zul).
 *
 * <p>
 * Migrasi dari legacy {@code TrialBalanceController.getGLAll()} +
 * {@code JournalTrxDAO.getTrialBalance()} yang memanggil fungsi database
 * {@code report.get_trial_balance(to_date(:tgl,'yyyy-MM-dd'))} dan menampilkan
 * kolom ACCT NO, ACCT NAME, DEBET, KREDIT, BALANCE.
 */
@Service
public class TrialBalanceService {

    private final JdbcTemplate jdbcTemplate;

    public TrialBalanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ambil data trial balance untuk periode tanggal tertentu. Migrasi dari
     * legacy {@code JournalTrxDAO.getTrialBalance()}.
     *
     * @param date tanggal ISO (yyyy-MM-dd)
     */
    public List<TrialBalanceRowResponse> getTrialBalance(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        return jdbcTemplate.query(
                "select v_acct_name, n_debit, n_credit, n_balance "
                        + "from report.get_trial_balance(to_date(?, 'yyyy-MM-dd')) "
                        + "where n_debit <> 0 or n_credit <> 0 "
                        + "order by v_acct_name",
                (resultSet, rowNum) -> parseRow(
                        resultSet.getString("v_acct_name"),
                        toDouble(resultSet.getObject("n_debit")),
                        toDouble(resultSet.getObject("n_credit")),
                        toDouble(resultSet.getObject("n_balance"))),
                date.trim());
    }

    /**
     * Parse "NAMA[NO_AKUN]" menjadi acctName + acctNo. Migrasi dari legacy
     * {@code TrialBalanceController.getAccount()}.
     */
    private TrialBalanceRowResponse parseRow(String value, Double debit, Double credit,
            Double balance) {
        String name = value;
        String no = "";
        if (value != null) {
            String normalized = value.replace("[", "&").replace("]", "");
            String[] parts = normalized.split("&");
            if (parts.length > 1) {
                name = parts[0];
                no = parts[1];
            } else {
                name = parts[0];
            }
        }
        return new TrialBalanceRowResponse(no, name, debit, credit, balance);
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
