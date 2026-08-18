package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan pendaftaran + total keseluruhan.
 */
public class LaporanPendaftaranResponse {

    private final int totalLakiLaki;
    private final int totalPerempuan;
    private final int totalLama;
    private final int totalBaru;
    private final int totalKeseluruhan;
    private final List<LaporanPendaftaranRowResponse> rows;

    public LaporanPendaftaranResponse(int totalLakiLaki, int totalPerempuan, int totalLama,
            int totalBaru, int totalKeseluruhan, List<LaporanPendaftaranRowResponse> rows) {
        this.totalLakiLaki = totalLakiLaki;
        this.totalPerempuan = totalPerempuan;
        this.totalLama = totalLama;
        this.totalBaru = totalBaru;
        this.totalKeseluruhan = totalKeseluruhan;
        this.rows = rows;
    }

    public int getTotalLakiLaki() {
        return totalLakiLaki;
    }

    public int getTotalPerempuan() {
        return totalPerempuan;
    }

    public int getTotalLama() {
        return totalLama;
    }

    public int getTotalBaru() {
        return totalBaru;
    }

    public int getTotalKeseluruhan() {
        return totalKeseluruhan;
    }

    public List<LaporanPendaftaranRowResponse> getRows() {
        return rows;
    }
}
