package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan penjualan pasien (RPT0001). Baris total ("TOTAL SELURUHNYA"
 * untuk rajal / "TOTAL" untuk ranap) sudah disertakan sebagai bagian dari
 * hasil fungsi database, sesuai perilaku legacy.
 */
public class PenjualanReportResponse {

    private final String tipe;
    private final String unitName;
    private final String shiftLabel;
    private final List<PenjualanReportRowResponse> rows;

    public PenjualanReportResponse(String tipe, String unitName, String shiftLabel,
            List<PenjualanReportRowResponse> rows) {
        this.tipe = tipe;
        this.unitName = unitName;
        this.shiftLabel = shiftLabel;
        this.rows = rows;
    }

    public String getTipe() {
        return tipe;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public List<PenjualanReportRowResponse> getRows() {
        return rows;
    }
}
