package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.ward.WardUnitResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0010 (LAPORAN PENDAFTARAN / laporanPendaftaran.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanPendaftaran} + {@code RajalManagerImpl.getRegistrationReport()}
 * + {@code TbRegistrationDAO.getLaporanPendaftaran()} — memakai fungsi database
 * {@code report.generate_laporan_pendaftaran} (semua unit) /
 * {@code report.laporan_pendaftaran_perunit} (per unit).
 */
@Service
public class LaporanPendaftaranService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public LaporanPendaftaranService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Unit untuk dropdown (unitId 0 = SEMUA di sisi frontend). */
    public List<WardUnitResponse> getUnits() {
        return jdbcTemplate.query(
                "select n_unit_id, v_unit_code, v_unit_name, n_whouse_id from ms_unit "
                        + "order by v_unit_name",
                (resultSet, rowNum) -> new WardUnitResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        getNullableInteger(resultSet, "n_whouse_id")));
    }

    /**
     * Laporan pendaftaran per rentang tanggal. Migrasi dari
     * {@code TbRegistrationDAO.getLaporanPendaftaran()}.
     */
    public LaporanPendaftaranResponse getReport(String from, String to, Integer unitId) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        boolean allUnits = unitId == null || unitId == 0;

        String sql;
        Object[] params;
        if (allUnits) {
            sql = "select lap.tanggal as tgl, lap.unit_name as unit, lap.jumlah_laki_laki as laki, "
                    + "lap.jumlah_perempuan as perempuan, lap.jumlah_lama as lama, "
                    + "lap.jumlah_baru as baru, lap.total as ttl "
                    + "from report.generate_laporan_pendaftaran(?, ?) lap order by tgl, unit";
            params = new Object[] { java.sql.Date.valueOf(fromDate), java.sql.Date.valueOf(toDate) };
        } else {
            sql = "select lap.tanggal as tgl, lap.unit_name as unit, lap.jumlah_laki_laki as laki, "
                    + "lap.jumlah_perempuan as perempuan, lap.jumlah_lama as lama, "
                    + "lap.jumlah_baru as baru, lap.total as ttl "
                    + "from report.laporan_pendaftaran_perunit(?, ?, ?) lap order by tgl, unit";
            params = new Object[] { java.sql.Date.valueOf(fromDate),
                    java.sql.Date.valueOf(toDate), unitId.shortValue() };
        }

        List<LaporanPendaftaranRowResponse> rows = jdbcTemplate.query(sql, params,
                (resultSet, rowNum) -> new LaporanPendaftaranRowResponse(
                        toDisplayDate(resultSet.getDate("tgl")),
                        resultSet.getString("unit"),
                        getNullableInteger(resultSet, "laki"),
                        getNullableInteger(resultSet, "perempuan"),
                        getNullableInteger(resultSet, "lama"),
                        getNullableInteger(resultSet, "baru"),
                        getNullableInteger(resultSet, "ttl")));

        int totLaki = 0;
        int totPerempuan = 0;
        int totLama = 0;
        int totBaru = 0;
        int totSemua = 0;
        for (LaporanPendaftaranRowResponse row : rows) {
            totLaki += row.getLakiLaki() == null ? 0 : row.getLakiLaki();
            totPerempuan += row.getPerempuan() == null ? 0 : row.getPerempuan();
            totLama += row.getLama() == null ? 0 : row.getLama();
            totBaru += row.getBaru() == null ? 0 : row.getBaru();
            totSemua += row.getTotal() == null ? 0 : row.getTotal();
        }
        return new LaporanPendaftaranResponse(totLaki, totPerempuan, totLama, totBaru, totSemua,
                rows);
    }

    private String toDisplayDate(java.sql.Date date) {
        return date == null ? "" : date.toLocalDate().format(DATE_DISPLAY);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }
}
