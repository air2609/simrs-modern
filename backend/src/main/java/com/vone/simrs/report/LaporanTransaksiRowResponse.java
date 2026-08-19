package com.vone.simrs.report;

/**
 * Baris laporan transaksi pasien poli/UGD (RPT0004). Migrasi dari legacy
 * {@code LaporanPoliUgd} + fungsi database {@code report.laporan_harian_poly_ugd}.
 */
public class LaporanTransaksiRowResponse {

    private final Integer nomor;
    private final String nomorNota;
    private final String namaPasien;
    private final String dokterUtama;
    private final Double biayaPeriksa;
    private final Double biayaTindakan;
    private final Double obatBm;

    public LaporanTransaksiRowResponse(Integer nomor, String nomorNota, String namaPasien,
            String dokterUtama, Double biayaPeriksa, Double biayaTindakan, Double obatBm) {
        this.nomor = nomor;
        this.nomorNota = nomorNota;
        this.namaPasien = namaPasien;
        this.dokterUtama = dokterUtama;
        this.biayaPeriksa = biayaPeriksa;
        this.biayaTindakan = biayaTindakan;
        this.obatBm = obatBm;
    }

    public Integer getNomor() {
        return nomor;
    }

    public String getNomorNota() {
        return nomorNota;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public String getDokterUtama() {
        return dokterUtama;
    }

    public Double getBiayaPeriksa() {
        return biayaPeriksa;
    }

    public Double getBiayaTindakan() {
        return biayaTindakan;
    }

    public Double getObatBm() {
        return obatBm;
    }
}
