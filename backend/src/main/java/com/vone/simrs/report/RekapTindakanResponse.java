package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan rekap tindakan + total qty / total nominal.
 */
public class RekapTindakanResponse {

    private final double totalQty;
    private final double totalNominal;
    private final List<RekapTindakanRowResponse> rows;

    public RekapTindakanResponse(double totalQty, double totalNominal,
            List<RekapTindakanRowResponse> rows) {
        this.totalQty = totalQty;
        this.totalNominal = totalNominal;
        this.rows = rows;
    }

    public double getTotalQty() {
        return totalQty;
    }

    public double getTotalNominal() {
        return totalNominal;
    }

    public List<RekapTindakanRowResponse> getRows() {
        return rows;
    }
}
