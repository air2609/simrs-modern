package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0016 (REKAP OBAT / rekapObat.zul).
 *
 * <p>
 * Migrasi dari legacy {@code RekapObat} (report) + {@code NoteManagerImpl.getRekapObat()}
 * + {@code NoteDAO.getRekapObatNew()} — rekap penjualan obat per rentang tanggal
 * dari tb_item_trx + tb_drug_ingredients (racikan).
 */
@Service
public class RekapObatReportService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public RekapObatReportService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Rekap penjualan obat per rentang tanggal. Migrasi {@code NoteDAO.getRekapObatNew()}.
     */
    public RekapObatReportResponse getReport(String from, String to) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("Kedua tanggal harus diisi....!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        Timestamp tgl1 = Timestamp.valueOf(fromDate.atStartOfDay());
        Timestamp tgl2 = Timestamp.valueOf(toDate.atTime(23, 59, 59));

        String sql = "select sum(qty) as jumlah, sum(n_amount_trx) as trx, "
                + "v_item_name as name from ( "
                + "select item.v_item_name, trx.n_qty as qty, trx.n_amount_trx "
                + "from tb_examination nota "
                + "join tb_item_trx trx on nota.n_exam_id = trx.n_note_id "
                + "join ms_item item on item.n_item_id = trx.n_item_id "
                + "where nota.d_whn_create between ? and ? and nota.n_exam_status = 2 "
                + "union all "
                + "select item.v_item_name, rd.n_dingr_det_qty as qty, "
                + "(select n_selling_price from ms_item_selling_price "
                + "where n_item_id = rd.n_item_id limit 1) * rd.n_dingr_det_qty as n_amount_trx "
                + "from tb_examination nota "
                + "join tb_drug_ingredients r on nota.n_exam_id = r.n_note_id "
                + "join tb_drug_ingredients_detail rd on r.n_dingr_id = rd.n_dingr_id "
                + "join ms_item item on item.n_item_id = rd.n_item_id "
                + "where nota.d_whn_create between ? and ? and nota.n_exam_status = 2 "
                + ") q group by q.v_item_name order by q.v_item_name";

        List<RekapObatReportRowResponse> rows = jdbcTemplate.query(sql,
                new Object[] { tgl1, tgl2, tgl1, tgl2 },
                (resultSet, rowNum) -> new RekapObatReportRowResponse(
                        resultSet.getString("name"),
                        resultSet.getDouble("jumlah"),
                        resultSet.getDouble("trx")));

        double total = 0;
        for (RekapObatReportRowResponse row : rows) {
            total += row.getNilaiPenjualan();
        }
        return new RekapObatReportResponse(total, rows);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
