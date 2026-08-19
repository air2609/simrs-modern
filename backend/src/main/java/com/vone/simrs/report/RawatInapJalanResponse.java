package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan rawat inap/jalan (RPT0015).
 */
public class RawatInapJalanResponse {

    private final String tipe;
    private final List<RawatInapJalanRowResponse> rows;

    public RawatInapJalanResponse(String tipe, List<RawatInapJalanRowResponse> rows) {
        this.tipe = tipe;
        this.rows = rows;
    }

    public String getTipe() {
        return tipe;
    }

    public List<RawatInapJalanRowResponse> getRows() {
        return rows;
    }
}
