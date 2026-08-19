package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan rekap obat (RPT0016) + total nilai penjualan
 * (field NILAI PENJUALAN pada legacy).
 */
public class RekapObatReportResponse {

    private final double totalNilaiPenjualan;
    private final List<RekapObatReportRowResponse> rows;

    public RekapObatReportResponse(double totalNilaiPenjualan,
            List<RekapObatReportRowResponse> rows) {
        this.totalNilaiPenjualan = totalNilaiPenjualan;
        this.rows = rows;
    }

    public double getTotalNilaiPenjualan() {
        return totalNilaiPenjualan;
    }

    public List<RekapObatReportRowResponse> getRows() {
        return rows;
    }
}
