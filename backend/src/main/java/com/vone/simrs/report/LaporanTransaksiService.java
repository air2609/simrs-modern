package com.vone.simrs.report;

import com.vone.simrs.auth.LegacyAuthService;
import com.vone.simrs.ward.WardUnitResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service untuk screen RPT0004 (LAPORAN TRANSAKSI PASIEN / laporanPasienPoliUgd.zul).
 *
 * <p>
 * Migrasi dari legacy {@code LaporanPoliUgd} — memakai fungsi database
 * {@code report.laporan_harian_poly_ugd(startdate, enddate, unitcode, shift)}.
 */
@Service
public class LaporanTransaksiService {

    private final JdbcTemplate jdbcTemplate;
    private final LegacyAuthService legacyAuthService;

    public LaporanTransaksiService(JdbcTemplate jdbcTemplate, LegacyAuthService legacyAuthService) {
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
     * Laporan transaksi harian poli/UGD per rentang tanggal + shift.
     *
     * @param from   tanggal awal (yyyy-MM-dd)
     * @param to     tanggal akhir (yyyy-MM-dd)
     * @param unitId unit terpilih
     * @param shift  2 (PAGI), 1 (SORE), 3 (MALAM), atau ALL
     */
    public LaporanTransaksiResponse getReport(String from, String to, Integer unitId, String shift) {
        if (!hasText(from) || !hasText(to)) {
            throw new IllegalArgumentException("TANGGAL HARUS DI ISI!");
        }
        if (unitId == null) {
            throw new IllegalArgumentException("LOKASI HARUS DIPILIH!");
        }
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        String shiftValue = hasText(shift) ? shift : "2";

        UnitRecord unit = findUnit(unitId);
        String mulai = fromDate + " 00:00:00";
        String sampai = toDate + " 23:59:59";
        String kodeUnit = "%" + unit.code + "%";

        String sql = "select l.nomor as nomor, l.nomor_nota as nota, l.nama_pasien as pasien, "
                + "l.dokter_utama as dokter, l.biaya_periksa as periksa, "
                + "l.biaya_tindakan as tindakan, l.obat_bm as obat_bm "
                + "from report.laporan_harian_poly_ugd(?, ?, ?, ?) l";

        List<LaporanTransaksiRowResponse> rows;
        if ("ALL".equalsIgnoreCase(shiftValue)) {
            // ALL SHIFT: gabungkan hasil fungsi utk shift 1 (SORE), 2 (PAGI), 3 (MALAM),
            // buang baris total per shift, lalu hitung satu baris total gabungan.
            rows = new ArrayList<>();
            double totPeriksa = 0;
            double totTindakan = 0;
            double totObat = 0;
            int nomor = 1;
            for (String singleShift : new String[] { "1", "2", "3" }) {
                List<LaporanTransaksiRowResponse> part = jdbcTemplate.query(sql,
                        new Object[] { mulai, sampai, kodeUnit, singleShift },
                        (resultSet, rowNum) -> new LaporanTransaksiRowResponse(
                                getNullableInteger(resultSet, "nomor"),
                                resultSet.getString("nota"),
                                resultSet.getString("pasien"),
                                resultSet.getString("dokter"),
                                toDouble(resultSet.getObject("periksa")),
                                toDouble(resultSet.getObject("tindakan")),
                                toDouble(resultSet.getObject("obat_bm"))));
                for (LaporanTransaksiRowResponse row : part) {
                    if (isTotalRow(row)) {
                        continue;
                    }
                    rows.add(new LaporanTransaksiRowResponse(nomor++, row.getNomorNota(),
                            row.getNamaPasien(), row.getDokterUtama(), row.getBiayaPeriksa(),
                            row.getBiayaTindakan(), row.getObatBm()));
                    totPeriksa += row.getBiayaPeriksa() == null ? 0 : row.getBiayaPeriksa();
                    totTindakan += row.getBiayaTindakan() == null ? 0 : row.getBiayaTindakan();
                    totObat += row.getObatBm() == null ? 0 : row.getObatBm();
                }
            }
            rows.add(new LaporanTransaksiRowResponse(null, null, "T O T A L", null,
                    totPeriksa, totTindakan, totObat));
        } else {
            rows = jdbcTemplate.query(sql,
                    new Object[] { mulai, sampai, kodeUnit, shiftValue },
                    (resultSet, rowNum) -> new LaporanTransaksiRowResponse(
                            getNullableInteger(resultSet, "nomor"),
                            resultSet.getString("nota"),
                            resultSet.getString("pasien"),
                            resultSet.getString("dokter"),
                            toDouble(resultSet.getObject("periksa")),
                            toDouble(resultSet.getObject("tindakan")),
                            toDouble(resultSet.getObject("obat_bm"))));
        }

        return new LaporanTransaksiResponse(unit.name, shiftLabel(shiftValue), rows);
    }

    private boolean isTotalRow(LaporanTransaksiRowResponse row) {
        String pasien = row.getNamaPasien();
        if (pasien == null) {
            return false;
        }
        // Tangani "T O T A L" (dengan spasi antar huruf) maupun "TOTAL"
        String normalized = pasien.replace(" ", "");
        return "TOTAL".equalsIgnoreCase(normalized);
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
