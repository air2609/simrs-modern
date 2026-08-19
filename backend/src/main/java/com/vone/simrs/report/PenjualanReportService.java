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
 * Service untuk screen RPT0001 (LAPORAN PENJUALAN PASIEN / laporanRajal.zul).
 *
 * <p>
 * Migrasi dari legacy {@code RajalReportingController} + {@code ItemManangerImpl}
 * + {@code ItemDAO.getRajalReport()/getRanapReport()} — memakai fungsi database
 * {@code report.laporan_penjualan_rajal} / {@code report.laporan_penjualan_all}
 * (rawat jalan) dan {@code report.laporan_penjualan_ranap} /
 * {@code report.laporan_penjualan_ranap_all} (rawat inap).
 */
@Service
public class PenjualanReportService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public PenjualanReportService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyAuthService = legacyAuthService;
    }

    public String requireUsername(HttpSession session) {
        return legacyAuthService.requireUsername(session);
    }

    /** Unit lokasi user (dropdown LOKASI), migrasi {@code UserManagerImpl.getUnitUser()}. */
    public List<WardUnitResponse> getUnits(String username) {
        return jdbcTemplate.query(
                "select distinct unt.n_unit_id, unt.v_unit_code, unt.v_unit_name, unt.n_whouse_id "
                        + "from ms_unit unt "
                        + "join ms_staff_in_unit stfunit on stfunit.n_unit_id = unt.n_unit_id "
                        + "join ms_user usr on usr.n_staff_id = stfunit.n_staff_id "
                        + "where upper(usr.v_user_name) = ? "
                        + "order by unt.v_unit_name",
                (resultSet, rowNum) -> new WardUnitResponse(
                        resultSet.getInt("n_unit_id"),
                        resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        getNullableInteger(resultSet, "n_whouse_id")),
                normalizeUsername(username));
    }

    /**
     * Laporan penjualan pasien rawat jalan / rawat inap.
     *
     * @param tipe   RAJAL atau RANAP
     * @param from   tanggal awal (yyyy-MM-dd)
     * @param to     tanggal akhir (yyyy-MM-dd)
     * @param unitId unit terpilih
     * @param shift  2 (PAGI), 1 (SORE), 3 (MALAM), atau ALL
     */
    public PenjualanReportResponse getReport(String tipe, String from, String to, Integer unitId,
            String shift) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        if (unitId == null) {
            throw new IllegalArgumentException("LOKASI HARUS DIPILIH!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        String pasType = hasText(tipe) ? tipe.toUpperCase() : "RAJAL";
        String shiftValue = hasText(shift) ? shift : "ALL";

        UnitRecord unit = findUnit(unitId);
        String mulai = fromDate + " 00:00:00";
        String sampai = toDate + " 23:59:59";

        List<PenjualanReportRowResponse> rows;
        if ("RANAP".equalsIgnoreCase(pasType)) {
            rows = getRanapReport(mulai, sampai, "I-" + unit.code + "%", shiftValue);
        } else {
            rows = getRajalReport(mulai, sampai, "J-" + unit.code + "%", shiftValue);
        }

        return new PenjualanReportResponse(pasType, unit.name, shiftLabel(shiftValue), rows);
    }

    /** Migrasi {@code ItemDAO.getRajalReport()}. */
    private List<PenjualanReportRowResponse> getRajalReport(String mulai, String sampai,
            String kodeRajal, String shift) {
        String function = "ALL".equalsIgnoreCase(shift)
                ? "report.laporan_penjualan_all(?, ?, ?, ?)"
                : "report.laporan_penjualan_rajal(?, ?, ?, ?)";
        String sql = "select r.nomor as no, r.no_nota as nota, r.no_resep as no_resep, "
                + "r.nama_pasien as pasien, r.total as total, r.diskon as diskon, "
                + "r.ppn as ppn, r.total_akhir as total_akhir from " + function + " r";
        return jdbcTemplate.query(sql,
                new Object[] { mulai, sampai, kodeRajal, shift },
                (resultSet, rowNum) -> new PenjualanReportRowResponse(
                        getNullableInteger(resultSet, "no"),
                        resultSet.getString("nota"),
                        resultSet.getString("no_resep"),
                        resultSet.getString("pasien"),
                        null, null, null, null,
                        toDouble(resultSet.getObject("total")),
                        toDouble(resultSet.getObject("diskon")),
                        toDouble(resultSet.getObject("ppn")),
                        toDouble(resultSet.getObject("total_akhir")),
                        null));
    }

    /** Migrasi {@code ItemDAO.getRanapReport()}. */
    private List<PenjualanReportRowResponse> getRanapReport(String mulai, String sampai,
            String kodeRajal, String shift) {
        boolean allShift = "ALL".equalsIgnoreCase(shift);
        String function = allShift
                ? "report.laporan_penjualan_ranap_all(?, ?, ?)"
                : "report.laporan_penjualan_ranap(?, ?, ?, ?)";
        Object[] params = allShift
                ? new Object[] { mulai, sampai, kodeRajal }
                : new Object[] { mulai, sampai, kodeRajal, shift };
        String sql = "select l.nomor as no, l.no_transaksi as nota, l.no_resep as no_resep, "
                + "l.no_registrasi as reg, l.nama_pasien as pasien, l.bed as bed, "
                + "l.ruangan as ruangan, l.r as r, l.total as total, l.diskon as diskon, "
                + "l.total_akhir as total_akhir, l.grup as grup from " + function + " l";
        return jdbcTemplate.query(sql, params,
                (resultSet, rowNum) -> new PenjualanReportRowResponse(
                        getNullableInteger(resultSet, "no"),
                        resultSet.getString("nota"),
                        resultSet.getString("no_resep"),
                        resultSet.getString("pasien"),
                        resultSet.getString("reg"),
                        resultSet.getString("bed"),
                        resultSet.getString("ruangan"),
                        getNullableInteger(resultSet, "r"),
                        toDouble(resultSet.getObject("total")),
                        toDouble(resultSet.getObject("diskon")),
                        null,
                        toDouble(resultSet.getObject("total_akhir")),
                        resultSet.getString("grup")));
    }

    private UnitRecord findUnit(Integer unitId) {
        List<UnitRecord> units = jdbcTemplate.query(
                "select v_unit_code, v_unit_name from ms_unit where n_unit_id = ?",
                (resultSet, rowNum) -> new UnitRecord(resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name")),
                unitId);
        if (units.isEmpty()) {
            throw new IllegalArgumentException("Unit tidak ditemukan.");
        }
        return units.get(0);
    }

    private String shiftLabel(String shiftValue) {
        switch (shiftValue.toUpperCase()) {
        case "1":
            return "SHIFT SORE";
        case "3":
            return "SHIFT MALAM";
        case "ALL":
            return "ALL SHIFT";
        default:
            return "SHIFT PAGI";
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase();
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

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String columnName)
            throws java.sql.SQLException {
        Number number = (Number) resultSet.getObject(columnName);
        return number == null ? null : number.intValue();
    }

    private static final class UnitRecord {
        private final String code;
        private final String name;

        private UnitRecord(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
