package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.ward.WardUnitResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0008 (LAPORAN PERSEDIAAN OBAT-BAHAN MEDIS / laporanPersediaanOBM.zul).
 *
 * <p>
 * Migrasi dari legacy {@code PersedianObat} + {@code ItemInventoryManagerImpl.getLaporanPersediaanObat()}
 * + {@code ItemInventoryDAO.getLaporanInventory()} — memakai fungsi database
 * {@code report.laporan_persediaan_obat(warehouseid)}.
 */
@Service
public class LaporanPersediaanService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public LaporanPersediaanService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
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
     * Persediaan obat-bahan medis per unit. Unit harus memiliki gudang
     * ({@code n_whouse_id}), sesuai validasi legacy
     * "{unitName} TIDAK MEMILIKI GUDANG OBAT-BAHAN MEDIS".
     */
    public LaporanPersediaanResponse getReport(Integer unitId) {
        if (unitId == null) {
            throw new IllegalArgumentException("LOKASI HARUS DIPILIH!");
        }
        UnitRecord unit = findUnit(unitId);
        if (unit.warehouseId == null) {
            throw new IllegalArgumentException(
                    unit.name + " TIDAK MEMILIKI GUDANG OBAT-BAHAN MEDIS");
        }

        String sql = "select nomor, kode_obat, nama_obat, harga_standar, jumlah, satuan "
                + "from report.laporan_persediaan_obat(?)";
        List<LaporanPersediaanRowResponse> rows = jdbcTemplate.query(sql,
                new Object[] { unit.warehouseId },
                (resultSet, rowNum) -> new LaporanPersediaanRowResponse(
                        getNullableInteger(resultSet, "nomor"),
                        resultSet.getString("kode_obat"),
                        resultSet.getString("nama_obat"),
                        toDouble(resultSet.getObject("harga_standar")),
                        toDouble(resultSet.getObject("jumlah")),
                        resultSet.getString("satuan")));

        return new LaporanPersediaanResponse(unit.name, unit.warehouseId, periodeLabel(), rows);
    }

    /** Nama bulan + tahun berjalan (mis. "AGUSTUS 2026"), sesuai legacy. */
    public String periodeLabel() {
        LocalDate now = LocalDate.now();
        String monthName = now.format(DateTimeFormatter.ofPattern("MMMM", new Locale("id")))
                .toUpperCase();
        return monthName + " " + now.getYear();
    }

    private UnitRecord findUnit(Integer unitId) {
        List<UnitRecord> units = jdbcTemplate.query(
                "select v_unit_code, v_unit_name, n_whouse_id from ms_unit where n_unit_id = ?",
                (resultSet, rowNum) -> new UnitRecord(resultSet.getString("v_unit_code"),
                        resultSet.getString("v_unit_name"),
                        getNullableInteger(resultSet, "n_whouse_id")),
                unitId);
        if (units.isEmpty()) {
            throw new IllegalArgumentException("Unit tidak ditemukan.");
        }
        return units.get(0);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toUpperCase();
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
        private final Integer warehouseId;

        private UnitRecord(String code, String name, Integer warehouseId) {
            this.code = code;
            this.name = name;
            this.warehouseId = warehouseId;
        }
    }
}
