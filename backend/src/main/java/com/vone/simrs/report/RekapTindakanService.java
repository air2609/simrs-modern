package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0011 (LAPORAN REKAP TINDAKAN / rekapTindakan.zul).
 *
 * <p>
 * Migrasi dari legacy {@code TreatmentReportController} + {@code TreatmentManagerImpl.getTreatmentReport()}
 * + {@code MsTreatmentDAO.getTreatmentReport()} — memakai fungsi database
 * {@code report.get_rekap_tindakan(d_from, d_to)}.
 */
@Service
public class RekapTindakanService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public RekapTindakanService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Rekap tindakan per rentang tanggal + tipe pasien (BPJS/COVID/NONBPJS/ALL).
     * Migrasi dari {@code MsTreatmentDAO.getTreatmentReport()}.
     */
    public RekapTindakanResponse getReport(String from, String to, String tipePasien) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        String pasType = hasText(tipePasien) ? tipePasien.toUpperCase() : "ALL";

        StringBuilder sql = new StringBuilder();
        sql.append("select kode as code, nama_tindakan as name, count(1) as qty, ")
                .append("sum(jasa_dokter) as amount ")
                .append("from report.get_rekap_tindakan(?, ?) ");
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(fromDate));
        params.add(java.sql.Date.valueOf(toDate));
        if (!"ALL".equals(pasType)) {
            sql.append("where tipe_pasien = ? ");
            params.add(pasType);
        }
        sql.append("group by kode, nama_tindakan order by nama_tindakan");

        List<RekapTindakanRowResponse> rows = jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> new RekapTindakanRowResponse(
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        toDouble(resultSet.getObject("qty")),
                        toDouble(resultSet.getObject("amount"))));

        double totQty = 0;
        double totNominal = 0;
        for (RekapTindakanRowResponse row : rows) {
            totQty += row.getQty();
            totNominal += row.getTotal();
        }
        return new RekapTindakanResponse(totQty, totNominal, rows);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
