package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan BOR (SC0073) + total bed / terisi / BOR keseluruhan.
 */
public class BorReportResponse {

    private final int totalBed;
    private final int totalTerisi;
    /** Nilai BOR total dalam persen. */
    private final double totalBor;
    private final List<BorReportRowResponse> rows;

    public BorReportResponse(int totalBed, int totalTerisi, double totalBor,
            List<BorReportRowResponse> rows) {
        this.totalBed = totalBed;
        this.totalTerisi = totalTerisi;
        this.totalBor = totalBor;
        this.rows = rows;
    }

    public int getTotalBed() {
        return totalBed;
    }

    public int getTotalTerisi() {
        return totalTerisi;
    }

    public double getTotalBor() {
        return totalBor;
    }

    public List<BorReportRowResponse> getRows() {
        return rows;
    }
}
