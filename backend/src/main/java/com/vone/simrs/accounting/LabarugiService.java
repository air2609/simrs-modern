package com.vone.simrs.accounting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0203 (LABA RUGI / labaRugi.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LabarugiController.cariClick()} +
 * {@code JournalTrxDAO.getLabarugi()} yang memanggil fungsi database
 * {@code report.profit_loss_bydate(:startDate, :endDate)}. Baris dikelompokkan
 * per {@code n_row}: 1 = INCOME, 2 = OTHER INCOME, 3 = COST OF GOODS SOLD,
 * 4 = EXPENSE, 5 = OTHER EXPENSE (caption dari {@code ms_coa_type.v_ct_name}),
 * 6 = "RINGKASAN" (baris TOTAL INCOME s.d. LABA BERSIH).
 */
@Service
public class LabarugiService {

    private final JdbcTemplate jdbcTemplate;

    public LabarugiService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ambil data laba rugi untuk rentang tanggal. Migrasi dari legacy
     * {@code JournalTrxDAO.getLabarugi()} + {@code LabarugiController.cariClick()}.
     *
     * @param from tanggal awal ISO (yyyy-MM-dd)
     * @param to   tanggal akhir ISO (yyyy-MM-dd)
     */
    public List<LabarugiGroupResponse> getLabarugi(String from, String to) {
        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        List<LabarugiItem> rows = jdbcTemplate.query(
                "select pl.n_row, pl.n_coa_id, pl.v_acct_name, pl.n_balance, ct.v_ct_name "
                        + "from report.profit_loss_bydate(to_date(?, 'yyyy-MM-dd'), to_date(?, 'yyyy-MM-dd')) pl "
                        + "left join ms_coa coa on coa.n_coa_id = pl.n_coa_id "
                        + "left join ms_coa_type ct on ct.n_ct_id = coa.n_type "
                        + "order by pl.n_row, pl.v_acct_name",
                (resultSet, rowNum) -> {
                    long row = resultSet.getLong("n_row");
                    String rawName = resultSet.getString("v_acct_name");
                    String[] account = parseAccount(rawName);
                    // Baris ringkasan (n_row >= 6): label di kolom NO. REKENING,
                    // NAMA kosong (sesuai legacy createItemTree(v_acct_name, "", ...)).
                    String acctNo = row >= 6 ? rawName : account[1];
                    String acctName = row >= 6 ? "" : account[0];
                    return new LabarugiItem(
                            row,
                            resultSet.getString("v_ct_name"),
                            acctNo,
                            acctName,
                            toDouble(resultSet.getObject("n_balance")));
                },
                from.trim(), to.trim());
        return groupByRow(rows);
    }

    /**
     * Kelompokkan baris menjadi grup per {@code n_row}. Migrasi dari legacy
     * {@code LabarugiController.cariClick()}: untuk n_row &lt; 6 caption grup
     * adalah {@code ms_coa_type.v_ct_name} dari COA pertama pada grup;
     * untuk n_row >= 6 captionnya "RINGKASAN" dan baris ringkasan memakai
     * {@code v_acct_name} sebagai NO. REKENING (nama kosong).
     */
    private List<LabarugiGroupResponse> groupByRow(List<LabarugiItem> rows) {
        Map<Long, List<LabarugiRowResponse>> grouped = new LinkedHashMap<>();
        Map<Long, String> captionByRow = new LinkedHashMap<>();
        for (LabarugiItem item : rows) {
            if (!captionByRow.containsKey(item.row)) {
                captionByRow.put(item.row, item.row >= 6 ? "RINGKASAN" : item.coaTypeName);
            }
            grouped.computeIfAbsent(item.row, key -> new ArrayList<>())
                    .add(new LabarugiRowResponse(item.acctNo, item.acctName, item.balance));
        }
        List<LabarugiGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<Long, List<LabarugiRowResponse>> entry : grouped.entrySet()) {
            groups.add(new LabarugiGroupResponse(
                    entry.getKey().intValue(),
                    captionByRow.get(entry.getKey()),
                    entry.getValue()));
        }
        return groups;
    }

    /**
     * Parse "NAMA [ NO_AKUN ]" menjadi [nama, no]. Migrasi dari legacy
     * yang membaca {@code v_acct_name} hasil fungsi report berformat
     * {@code v_acct_name || ' [ ' || v_acct_no || ' ]'}. Baris ringkasan
     * (TOTAL INCOME, LABA BERSIH, dll.) tidak berformat bracket, sehingga
     * seluruh teks menjadi nama.
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

    /** Baris laba rugi internal (sebelum dikelompokkan). */
    private static final class LabarugiItem {
        private final long row;
        private final String coaTypeName;
        private final String acctNo;
        private final String acctName;
        private final Double balance;

        private LabarugiItem(long row, String coaTypeName, String acctNo, String acctName,
                Double balance) {
            this.row = row;
            this.coaTypeName = coaTypeName;
            this.acctNo = acctNo;
            this.acctName = acctName;
            this.balance = balance;
        }
    }
}
