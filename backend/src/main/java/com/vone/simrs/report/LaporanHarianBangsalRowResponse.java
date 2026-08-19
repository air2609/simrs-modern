package com.vone.simrs.report;

/**
 * Baris rincian rekap pasien bangsal (RPT0006). Migrasi dari legacy
 * fungsi database {@code report.fungsi_rekap_pasien_bangsal}.
 */
public class LaporanHarianBangsalRowResponse {

    private final Integer nomor;
    private final String nomorTransaksi;
    private final String kodeTransaksi;
    private final String keterangan;
    private final Integer jumlah;
    private final Double nilai;

    public LaporanHarianBangsalRowResponse(Integer nomor, String nomorTransaksi,
            String kodeTransaksi, String keterangan, Integer jumlah, Double nilai) {
        this.nomor = nomor;
        this.nomorTransaksi = nomorTransaksi;
        this.kodeTransaksi = kodeTransaksi;
        this.keterangan = keterangan;
        this.jumlah = jumlah;
        this.nilai = nilai;
    }

    public Integer getNomor() {
        return nomor;
    }

    public String getNomorTransaksi() {
        return nomorTransaksi;
    }

    public String getKodeTransaksi() {
        return kodeTransaksi;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public Integer getJumlah() {
        return jumlah;
    }

    public Double getNilai() {
        return nilai;
    }
}
