package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan rekap kasir + total tunai / card / nontunai.
 */
public class RekapKasirResponse {

    private final double totalTunai;
    private final double totalCard;
    private final double totalNontunai;
    private final List<RekapKasirRowResponse> rows;

    public RekapKasirResponse(double totalTunai, double totalCard, double totalNontunai,
            List<RekapKasirRowResponse> rows) {
        this.totalTunai = totalTunai;
        this.totalCard = totalCard;
        this.totalNontunai = totalNontunai;
        this.rows = rows;
    }

    public double getTotalTunai() {
        return totalTunai;
    }

    public double getTotalCard() {
        return totalCard;
    }

    public double getTotalNontunai() {
        return totalNontunai;
    }

    public List<RekapKasirRowResponse> getRows() {
        return rows;
    }
}
