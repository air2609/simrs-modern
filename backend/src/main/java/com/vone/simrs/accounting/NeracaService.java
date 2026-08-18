package com.vone.simrs.accounting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0202 (NERACA / neraca.zul).
 *
 * <p>
 * Migrasi dari legacy {@code NeracaController.openNeraca()} +
 * {@code JournalTrxDAO.getNeracaByDate()} yang memanggil fungsi database
 * {@code report.get_neraca_by_date(to_date(:tgl,'dd/MM/yyyy'))} dan
 * menampilkan grup AKTIVA / KEWAJIBAN / MODAL berisi akun dengan kolom
 * NO. REKENING, NAMA, SALDO.
 */
@Service
public class NeracaService {

    private final JdbcTemplate jdbcTemplate;

    public NeracaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ambil data neraca per tanggal periode. Migrasi dari legacy
     * {@code JournalTrxDAO.getNeracaByDate()} + {@code NeracaController.openNeraca()}.
     *
     * @param date tanggal ISO (yyyy-MM-dd)
     */
    public List<NeracaGroupResponse> getNeraca(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        List<NeracaItem> rows = jdbcTemplate.query(
                "select n_row, v_acct_name, n_balance "
                        + "from report.get_neraca_by_date(to_date(?, 'yyyy-MM-dd')) "
                        + "order by n_row, v_acct_name",
                (resultSet, rowNum) -> {
                    String[] account = parseAccount(resultSet.getString("v_acct_name"));
                    return new NeracaItem(
                            resultSet.getLong("n_row"),
                            account[1],
                            account[0],
                            toDouble(resultSet.getObject("n_balance")));
                },
                date.trim());
        return groupByRow(rows);
    }

    /**
     * Kelompokkan baris menjadi grup AKTIVA (1), KEWAJIBAN (2), MODAL (3).
     * Migrasi dari legacy {@code NeracaController.getCaption()} +
     * {@code createGroupTree()}. n_row dari fungsi DB: 1 = AKTIVA,
     * 2 = HUTANG/KEWAJIBAN, 3 = EQUITY/MODAL.
     */
    private List<NeracaGroupResponse> groupByRow(List<NeracaItem> rows) {
        Map<Long, List<NeracaRowResponse>> grouped = new LinkedHashMap<>();
        for (NeracaItem item : rows) {
            grouped.computeIfAbsent(item.row, key -> new ArrayList<>())
                    .add(new NeracaRowResponse(item.acctNo, item.acctName, item.balance));
        }
        List<NeracaGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<Long, List<NeracaRowResponse>> entry : grouped.entrySet()) {
            groups.add(new NeracaGroupResponse(
                    entry.getKey().intValue(),
                    caption(entry.getKey()),
                    entry.getValue()));
        }
        return groups;
    }

    /**
     * Caption grup sesuai legacy {@code NeracaController.getCaption()}.
     */
    private String caption(long row) {
        if (row == 1) {
            return "AKTIVA";
        }
        if (row == 2) {
            return "KEWAJIBAN";
        }
        if (row == 3) {
            return "MODAL";
        }
        return "";
    }

    /**
     * Parse "NAMA [ NO_AKUN ]" menjadi [nama, no]. Migrasi dari legacy
     * yang membaca {@code v_acct_name} hasil fungsi report berformat
     * {@code v_acct_name || ' [ ' || v_acct_no || ' ]'}.
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

    /** Baris neraca internal (sebelum dikelompokkan). */
    private static final class NeracaItem {
        private final long row;
        private final String acctNo;
        private final String acctName;
        private final Double balance;

        private NeracaItem(long row, String acctNo, String acctName, Double balance) {
            this.row = row;
            this.acctNo = acctNo;
            this.acctName = acctName;
            this.balance = balance;
        }
    }
}
