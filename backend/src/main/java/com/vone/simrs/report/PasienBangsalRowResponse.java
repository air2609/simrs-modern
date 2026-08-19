package com.vone.simrs.report;

/**
 * Baris laporan pasien masuk bangsal (RPT0005). Migrasi dari legacy
 * fungsi database {@code report.laporan_pasien_masuk_bangsal} (tipe
 * {@code report.laporan_pasien_masuk_ranap}).
 */
public class PasienBangsalRowResponse {

    private final int nomor;
    private final String noRegistrasi;
    private final String noRm;
    private final String namaPasien;
    private final String tglDaftar;
    private final String namaBed;
    private final String jenisPasien;
    private final String alamatPasien;
    private final String ruangan;

    public PasienBangsalRowResponse(int nomor, String noRegistrasi, String noRm,
            String namaPasien, String tglDaftar, String namaBed, String jenisPasien,
            String alamatPasien, String ruangan) {
        this.nomor = nomor;
        this.noRegistrasi = noRegistrasi;
        this.noRm = noRm;
        this.namaPasien = namaPasien;
        this.tglDaftar = tglDaftar;
        this.namaBed = namaBed;
        this.jenisPasien = jenisPasien;
        this.alamatPasien = alamatPasien;
        this.ruangan = ruangan;
    }

    public int getNomor() {
        return nomor;
    }

    public String getNoRegistrasi() {
        return noRegistrasi;
    }

    public String getNoRm() {
        return noRm;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public String getTglDaftar() {
        return tglDaftar;
    }

    public String getNamaBed() {
        return namaBed;
    }

    public String getJenisPasien() {
        return jenisPasien;
    }

    public String getAlamatPasien() {
        return alamatPasien;
    }

    public String getRuangan() {
        return ruangan;
    }
}
