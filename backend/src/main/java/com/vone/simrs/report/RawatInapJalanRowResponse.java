package com.vone.simrs.report;

/**
 * Baris laporan rawat inap/jalan (RPT0015). Migrasi dari legacy
 * {@code NoteManagerImpl.getRawatInapJalan()}.
 *
 * <p>Untuk RI: mrNo, namaPasien, jk, tglLahir, usia, tipe, status, agama, etnis,
 * bahasa, dokter, bed, kelas, tglMasuk, tglKeluar, lama, diagnosa.
 * Untuk RJ: mrNo, namaPasien, jk, tglLahir, usia, tipe, status, agama, etnis,
 * bahasa, tglDaftar, unit, dokter, diagnosa.
 */
public class RawatInapJalanRowResponse {

    private final String mrNo;
    private final String namaPasien;
    private final String jk;
    private final String tglLahir;
    private final double usia;
    private final String tipe;
    private final String status;
    private final String agama;
    private final String etnis;
    private final String bahasa;
    private final String dokter;
    private final String bed;
    private final String kelas;
    private final String tglMasuk;
    private final String tglKeluar;
    private final int lama;
    private final String diagnosa;
    private final String tglDaftar;
    private final String unit;

    public RawatInapJalanRowResponse(String mrNo, String namaPasien, String jk, String tglLahir,
            double usia, String tipe, String status, String agama, String etnis, String bahasa,
            String dokter, String bed, String kelas, String tglMasuk, String tglKeluar, int lama,
            String diagnosa, String tglDaftar, String unit) {
        this.mrNo = mrNo;
        this.namaPasien = namaPasien;
        this.jk = jk;
        this.tglLahir = tglLahir;
        this.usia = usia;
        this.tipe = tipe;
        this.status = status;
        this.agama = agama;
        this.etnis = etnis;
        this.bahasa = bahasa;
        this.dokter = dokter;
        this.bed = bed;
        this.kelas = kelas;
        this.tglMasuk = tglMasuk;
        this.tglKeluar = tglKeluar;
        this.lama = lama;
        this.diagnosa = diagnosa;
        this.tglDaftar = tglDaftar;
        this.unit = unit;
    }

    public String getMrNo() {
        return mrNo;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public String getJk() {
        return jk;
    }

    public String getTglLahir() {
        return tglLahir;
    }

    public double getUsia() {
        return usia;
    }

    public String getTipe() {
        return tipe;
    }

    public String getStatus() {
        return status;
    }

    public String getAgama() {
        return agama;
    }

    public String getEtnis() {
        return etnis;
    }

    public String getBahasa() {
        return bahasa;
    }

    public String getDokter() {
        return dokter;
    }

    public String getBed() {
        return bed;
    }

    public String getKelas() {
        return kelas;
    }

    public String getTglMasuk() {
        return tglMasuk;
    }

    public String getTglKeluar() {
        return tglKeluar;
    }

    public int getLama() {
        return lama;
    }

    public String getDiagnosa() {
        return diagnosa;
    }

    public String getTglDaftar() {
        return tglDaftar;
    }

    public String getUnit() {
        return unit;
    }
}
