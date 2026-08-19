package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0012 (LAPORAN REKAP PENERIMAAN KASIR / laporanKasir.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanPenerimaanKasir} + {@code CashierManagerImpl.getPatientSettlement()}
 * + {@code CashierDAO.getPatientBill()} — rekap kwitansi pasien per tanggal & shift,
 * tunai = jumlah settlement tipe 3, nontunai = settlement selain tipe 3.
 */
@Service
public class PenerimaanKasirService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public PenerimaanKasirService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Rekap penerimaan kasir per tanggal & shift (2 PAGI, 1 SORE, 3 MALAM, A ALL).
     * Migrasi {@code CashierDAO.getPatientBill()} + logika subtotal
     * {@code CashierManagerImpl.getPatientSettlement()}.
     */
    public PenerimaanKasirResponse getReport(String date, String shift) {
        if (!hasText(date)) {
            throw new IllegalArgumentException("Isi tanggal terlebih dahulu");
        }
        LocalDate transDate = LocalDate.parse(date);
        LocalDate nextDate = transDate.plusDays(1);
        String shiftValue = hasText(shift) ? shift : "2";

        StringBuilder sql = new StringBuilder();
        sql.append("select b.v_pbill_code as kwitansi, b.d_settlement_date as tgl, ")
                .append("b.v_who_create as kasir, ")
                .append("coalesce(sum(case when s.n_psettlement_type = 3 ")
                .append("then s.n_amount_settled else 0 end), 0) as tunai, ")
                .append("coalesce(sum(case when s.n_psettlement_type <> 3 ")
                .append("then s.n_amount_settled else 0 end), 0) as nontunai ")
                .append("from tb_patient_bill b ")
                .append("left join tb_patient_settlement s on s.n_pbill_id = b.n_pbill_id ")
                .append("where b.d_whn_create >= ? and b.d_whn_create < ? ");
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Timestamp.valueOf(transDate.atStartOfDay()));
        params.add(java.sql.Timestamp.valueOf(nextDate.atStartOfDay()));
        if (!"A".equalsIgnoreCase(shiftValue)) {
            sql.append("and b.id_shift = ? ");
            params.add(Short.valueOf(shiftValue));
        }
        sql.append("group by b.n_pbill_id, b.v_pbill_code, b.d_settlement_date, b.v_who_create ")
                .append("order by b.v_who_create, b.v_pbill_code");

        List<BillRow> bills = jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> new BillRow(
                        resultSet.getString("kwitansi"),
                        resultSet.getTimestamp("tgl") == null ? ""
                                : resultSet.getTimestamp("tgl").toLocalDateTime()
                                        .toLocalDate().format(DATE_DISPLAY),
                        resultSet.getString("kasir"),
                        resultSet.getDouble("tunai"),
                        resultSet.getDouble("nontunai")));

        // Logika tampilan legacy: subtotal "TOTAL" saat kasir berganti (hanya jika
        // tunaiPerCashier > 0), lalu baris TOTAL akhir dari sisa akumulasi.
        List<PenerimaanKasirRowResponse> rows = new ArrayList<>();
        double totalTunai = 0;
        double totalNonTunai = 0;
        String whoCreated = "";
        double tunaiPerCashier = 0;
        double nonTunaiPerCashier = 0;
        int index = 0;
        for (BillRow bill : bills) {
            if (index == 0) {
                whoCreated = bill.kasir;
            }
            if (!bill.kasir.equalsIgnoreCase(whoCreated)) {
                if (tunaiPerCashier > 0) {
                    rows.add(new PenerimaanKasirRowResponse("", "TOTAL",
                            tunaiPerCashier, nonTunaiPerCashier, ""));
                }
                tunaiPerCashier = 0;
                nonTunaiPerCashier = 0;
                whoCreated = bill.kasir;
            }
            rows.add(new PenerimaanKasirRowResponse(bill.tgl, bill.kwitansi, bill.tunai,
                    bill.nontunai, bill.kasir));
            tunaiPerCashier += bill.tunai;
            nonTunaiPerCashier += bill.nontunai;
            totalTunai += bill.tunai;
            totalNonTunai += bill.nontunai;
            index++;
        }
        rows.add(new PenerimaanKasirRowResponse("", "TOTAL", tunaiPerCashier,
                nonTunaiPerCashier, ""));

        return new PenerimaanKasirResponse(totalTunai, totalNonTunai, rows);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class BillRow {
        private final String kwitansi;
        private final String tgl;
        private final String kasir;
        private final double tunai;
        private final double nontunai;

        private BillRow(String kwitansi, String tgl, String kasir, double tunai,
                double nontunai) {
            this.kwitansi = kwitansi;
            this.tgl = tgl;
            this.kasir = kasir;
            this.tunai = tunai;
            this.nontunai = nontunai;
        }
    }
}
