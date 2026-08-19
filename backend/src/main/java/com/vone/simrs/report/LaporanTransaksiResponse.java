package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan transaksi pasien poli/UGD (RPT0004). Baris total ("T O T A L")
 * sudah disertakan sebagai bagian dari hasil fungsi database, sesuai legacy.
 */
public class LaporanTransaksiResponse {

    private final String unitName;
    private final String shiftLabel;
    private final List<LaporanTransaksiRowResponse> rows;

    public LaporanTransaksiResponse(String unitName, String shiftLabel,
            List<LaporanTransaksiRowResponse> rows) {
        this.unitName = unitName;
        this.shiftLabel = shiftLabel;
        this.rows = rows;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public List<LaporanTransaksiRowResponse> getRows() {
        return rows;
    }
}
