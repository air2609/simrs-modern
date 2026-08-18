package com.vone.simrs.report;

/**
 * Baris laporan rekap kasir (RPT0022). Migrasi dari legacy
 * {@code CashierDAO.getRekapBillFunction()}.
 */
public class RekapKasirRowResponse {

    private final String tanggal;
    private final String kwitansi;
    private final String namaPasien;
    private final String mrNo;
    private final String tipePasien;
    private final String kelasTarif;
    private final String tglMasuk;
    private final String tglKeluar;
    private final String dokter;
    private final Double total;
    private final Double tunai;
    private final Double card;
    private final Double nontunai;
    private final String bank;
    private final String perusahaan;

    public RekapKasirRowResponse(String tanggal, String kwitansi, String namaPasien, String mrNo,
            String tipePasien, String kelasTarif, String tglMasuk, String tglKeluar, String dokter,
            Double total, Double tunai, Double card, Double nontunai, String bank,
            String perusahaan) {
        this.tanggal = tanggal;
        this.kwitansi = kwitansi;
        this.namaPasien = namaPasien;
        this.mrNo = mrNo;
        this.tipePasien = tipePasien;
        this.kelasTarif = kelasTarif;
        this.tglMasuk = tglMasuk;
        this.tglKeluar = tglKeluar;
        this.dokter = dokter;
        this.total = total;
        this.tunai = tunai;
        this.card = card;
        this.nontunai = nontunai;
        this.bank = bank;
        this.perusahaan = perusahaan;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getKwitansi() {
        return kwitansi;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public String getMrNo() {
        return mrNo;
    }

    public String getTipePasien() {
        return tipePasien;
    }

    public String getKelasTarif() {
        return kelasTarif;
    }

    public String getTglMasuk() {
        return tglMasuk;
    }

    public String getTglKeluar() {
        return tglKeluar;
    }

    public String getDokter() {
        return dokter;
    }

    public Double getTotal() {
        return total;
    }

    public Double getTunai() {
        return tunai;
    }

    public Double getCard() {
        return card;
    }

    public Double getNontunai() {
        return nontunai;
    }

    public String getBank() {
        return bank;
    }

    public String getPerusahaan() {
        return perusahaan;
    }
}
