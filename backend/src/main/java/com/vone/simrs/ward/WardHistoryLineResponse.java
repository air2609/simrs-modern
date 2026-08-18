package com.vone.simrs.ward;

/**
 * Baris riwayat (KETERANGAN, SUB DIVISI, TANGGAL, JUMLAH).
 */
public class WardHistoryLineResponse {

    private final String keterangan;
    private final String subDivisi;
    private final String tanggal;
    private final double jumlah;

    public WardHistoryLineResponse(String keterangan, String subDivisi, String tanggal,
            double jumlah) {
        this.keterangan = keterangan;
        this.subDivisi = subDivisi;
        this.tanggal = tanggal;
        this.jumlah = jumlah;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public String getSubDivisi() {
        return subDivisi;
    }

    public String getTanggal() {
        return tanggal;
    }

    public double getJumlah() {
        return jumlah;
    }
}
