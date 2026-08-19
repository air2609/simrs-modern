package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan penerimaan kasir (RPT0012) + total tunai / nontunai keseluruhan
 * (ditampilkan legacy di field JUMLAH TUNAI / JUMLAH NONTUNAI).
 */
public class PenerimaanKasirResponse {

    private final double totalTunai;
    private final double totalNonTunai;
    private final List<PenerimaanKasirRowResponse> rows;

    public PenerimaanKasirResponse(double totalTunai, double totalNonTunai,
            List<PenerimaanKasirRowResponse> rows) {
        this.totalTunai = totalTunai;
        this.totalNonTunai = totalNonTunai;
        this.rows = rows;
    }

    public double getTotalTunai() {
        return totalTunai;
    }

    public double getTotalNonTunai() {
        return totalNonTunai;
    }

    public List<PenerimaanKasirRowResponse> getRows() {
        return rows;
    }
}
