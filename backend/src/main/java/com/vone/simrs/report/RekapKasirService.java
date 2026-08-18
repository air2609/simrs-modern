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
 * Service untuk screen RPT0022 (LAPORAN REKAP KASIR / rekapAllKasir.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanRekapKasir} + {@code CashierManagerImpl.getRekapKasir()}
 * + {@code CashierDAO.getRekapBillFunction()} — memakai fungsi database
 * {@code report.get_rekap_kasir(d_from, d_to)}.
 */
@Service
public class RekapKasirService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public RekapKasirService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /**
     * Laporan rekap kasir per rentang tanggal + filter laporan (RJ/RI/ALL)
     * dan tipe pasien (BPJS/COVID/NONBPJS/ALL).
     */
    public RekapKasirResponse getReport(String from, String to, String laporanType,
            String pasienType) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("Kedua tanggal harus diisi....!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        String lapType = hasText(laporanType) ? laporanType.toUpperCase() : "ALL";
        String pasType = hasText(pasienType) ? pasienType.toUpperCase() : "ALL";

        StringBuilder sql = new StringBuilder();
        sql.append("select tanggal as tgl, nomor_kwitansi as kwitansi, nama_pasien as nama, ")
                .append("nomor_mr as mr, tipe_pasien as tipe, kelas_tarif as kelas, ")
                .append("tanggal_masuk as masuk, tanggal_keluar as keluar, nama_dokter as dokter, ")
                .append("total as ttl, tunai as tunai, non_tunai_bank as card, ")
                .append("non_tunai_perusahaan as ins, nama_bank as bank, nama_perusahaan as perusahaan ")
                .append("from report.get_rekap_kasir(?, ?) ");
        List<Object> params = new ArrayList<>();
        params.add(java.sql.Date.valueOf(fromDate));
        params.add(java.sql.Date.valueOf(toDate));
        if (!"ALL".equals(lapType)) {
            sql.append("where tipe_registrasi = ? ");
            params.add(lapType);
            if (!"ALL".equals(pasType)) {
                sql.append("and tipe_pasien = ? ");
                params.add(pasType);
            }
        } else if (!"ALL".equals(pasType)) {
            sql.append("where tipe_pasien = ? ");
            params.add(pasType);
        }
        sql.append("order by tgl desc, kwitansi");

        List<RekapKasirRowResponse> rows = jdbcTemplate.query(sql.toString(), params.toArray(),
                (resultSet, rowNum) -> new RekapKasirRowResponse(
                        toDisplayDate(resultSet.getDate("tgl")),
                        resultSet.getString("kwitansi"),
                        resultSet.getString("nama"),
                        resultSet.getString("mr"),
                        resultSet.getString("tipe"),
                        resultSet.getString("kelas"),
                        toDisplayDateTime(resultSet.getTimestamp("masuk")),
                        resultSet.getTimestamp("keluar") == null ? ""
                                : toDisplayDateTime(resultSet.getTimestamp("keluar")),
                        resultSet.getString("dokter"),
                        toDouble(resultSet.getObject("ttl")),
                        toDouble(resultSet.getObject("tunai")),
                        toDouble(resultSet.getObject("card")),
                        toDouble(resultSet.getObject("ins")),
                        resultSet.getString("bank"),
                        resultSet.getString("perusahaan")));

        double totTunai = 0;
        double totCard = 0;
        double totNontunai = 0;
        for (RekapKasirRowResponse row : rows) {
            totTunai += row.getTunai() == null ? 0 : row.getTunai();
            totCard += row.getCard() == null ? 0 : row.getCard();
            totNontunai += row.getNontunai() == null ? 0 : row.getNontunai();
        }
        return new RekapKasirResponse(totTunai, totCard, totNontunai, rows);
    }

    private String toDisplayDate(java.sql.Date date) {
        return date == null ? "" : date.toLocalDate().format(DATE_DISPLAY);
    }

    private String toDisplayDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DATE_DISPLAY);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
