package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen SC0073 (BOR REPORT / borReport.zul).
 *
 * <p>
 * Migrasi dari legacy {@code BorController} + {@code BedManagerImpl.getBorReport()}
 * + {@code MsBedDAO.getBorInfo()} — BOR (Bed Occupancy Rate) per kelas tarif &
 * ruangan untuk rentang tanggal tertentu.
 */
@Service
public class BorReportService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public BorReportService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * BOR per kelas tarif & ruangan. Migrasi dari {@code MsBedDAO.getBorInfo()}:
     * total bed dari {@code tb_hall_bed.quantity} (record_date), bed terisi dari
     * {@code tb_bor} (bed_date), BOR = terisi / total.
     */
    public BorReportResponse getReport(String from, String to) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        String sql = "select q.kelas, q.ruangan, q.total, q.terpakai, "
                + "cast(terpakai as float4) / cast(total as float4) as bor "
                + "from (select c.v_tclass_desc as kelas, h.v_hall_name as ruangan, "
                + "hb.tclass_id, hb.hall_id, sum(hb.quantity) as total, "
                + "(select count(1) from tb_bor where tclass_id = hb.tclass_id "
                + "and hall_id = hb.hall_id and bed_date between ? and ?) as terpakai "
                + "from tb_hall_bed hb "
                + "join ms_treatment_class c on c.n_tclass_id = hb.tclass_id "
                + "join ms_hall h on h.n_hall_id = hb.hall_id "
                + "where hb.record_date between ? and ? "
                + "group by kelas, ruangan, hb.tclass_id, hb.hall_id) q "
                + "order by q.kelas, q.ruangan";

        Object[] params = new Object[] { java.sql.Date.valueOf(fromDate),
                java.sql.Date.valueOf(toDate), java.sql.Date.valueOf(fromDate),
                java.sql.Date.valueOf(toDate) };

        List<BorReportRowResponse> rows = new ArrayList<>();
        int[] totals = new int[2]; // [0] totalBed, [1] totalTerisi
        jdbcTemplate.query(sql, params, resultSet -> {
            int bed = resultSet.getInt("total");
            int terisi = resultSet.getInt("terpakai");
            double bor = resultSet.getDouble("bor") * 100;
            rows.add(new BorReportRowResponse(resultSet.getString("kelas"),
                    resultSet.getString("ruangan"), bed, terisi, bor));
            totals[0] += bed;
            totals[1] += terisi;
        });

        int totalBed = totals[0];
        int totalTerisi = totals[1];

        double totalBor = totalBed > 0 ? ((double) totalTerisi / (double) totalBed) * 100 : 0;
        return new BorReportResponse(totalBed, totalTerisi, totalBor, rows);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
