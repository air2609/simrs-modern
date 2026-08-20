package com.vone.simrs.master.patient;

import java.time.LocalDate;

/**
 * Request simpan/ubah data pasien (SCM0011). Migrasi legacy
 * {@code PatientController.doSaveAdd()}.
 */
public class PatientSaveRequest {

    private String mrCode;
    private String namaPasien;
    private String jenisKelamin; // M / F
    private LocalDate tglLahir;
    private String agama;
    private String wargaNegara;
    private String statusKawin;
    private String alamat;
    private String rt;
    private String rw;
    private Integer kelurahanId;
    private Integer kecamatanId;
    private Integer kabupatenId;
    private Integer propinsiId;
    private String noTelp;
    private String alamatAlternatif;
    private String rt1;
    private String rw1;
    private String noTelpAlt;
    private String pendidikan;
    private String jenisPekerjaan;
    private Integer tipePasienId;
    private String prioritas;

    public String getMrCode() {
        return mrCode;
    }

    public void setMrCode(String mrCode) {
        this.mrCode = mrCode;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public void setNamaPasien(String namaPasien) {
        this.namaPasien = namaPasien;
    }

    public String getJenisKelamin() {
        return jenisKelamin;
    }

    public void setJenisKelamin(String jenisKelamin) {
        this.jenisKelamin = jenisKelamin;
    }

    public LocalDate getTglLahir() {
        return tglLahir;
    }

    public void setTglLahir(LocalDate tglLahir) {
        this.tglLahir = tglLahir;
    }

    public String getAgama() {
        return agama;
    }

    public void setAgama(String agama) {
        this.agama = agama;
    }

    public String getWargaNegara() {
        return wargaNegara;
    }

    public void setWargaNegara(String wargaNegara) {
        this.wargaNegara = wargaNegara;
    }

    public String getStatusKawin() {
        return statusKawin;
    }

    public void setStatusKawin(String statusKawin) {
        this.statusKawin = statusKawin;
    }

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public String getRt() {
        return rt;
    }

    public void setRt(String rt) {
        this.rt = rt;
    }

    public String getRw() {
        return rw;
    }

    public void setRw(String rw) {
        this.rw = rw;
    }

    public Integer getKelurahanId() {
        return kelurahanId;
    }

    public void setKelurahanId(Integer kelurahanId) {
        this.kelurahanId = kelurahanId;
    }

    public Integer getKecamatanId() {
        return kecamatanId;
    }

    public void setKecamatanId(Integer kecamatanId) {
        this.kecamatanId = kecamatanId;
    }

    public Integer getKabupatenId() {
        return kabupatenId;
    }

    public void setKabupatenId(Integer kabupatenId) {
        this.kabupatenId = kabupatenId;
    }

    public Integer getPropinsiId() {
        return propinsiId;
    }

    public void setPropinsiId(Integer propinsiId) {
        this.propinsiId = propinsiId;
    }

    public String getNoTelp() {
        return noTelp;
    }

    public void setNoTelp(String noTelp) {
        this.noTelp = noTelp;
    }

    public String getAlamatAlternatif() {
        return alamatAlternatif;
    }

    public void setAlamatAlternatif(String alamatAlternatif) {
        this.alamatAlternatif = alamatAlternatif;
    }

    public String getRt1() {
        return rt1;
    }

    public void setRt1(String rt1) {
        this.rt1 = rt1;
    }

    public String getRw1() {
        return rw1;
    }

    public void setRw1(String rw1) {
        this.rw1 = rw1;
    }

    public String getNoTelpAlt() {
        return noTelpAlt;
    }

    public void setNoTelpAlt(String noTelpAlt) {
        this.noTelpAlt = noTelpAlt;
    }

    public String getPendidikan() {
        return pendidikan;
    }

    public void setPendidikan(String pendidikan) {
        this.pendidikan = pendidikan;
    }

    public String getJenisPekerjaan() {
        return jenisPekerjaan;
    }

    public void setJenisPekerjaan(String jenisPekerjaan) {
        this.jenisPekerjaan = jenisPekerjaan;
    }

    public Integer getTipePasienId() {
        return tipePasienId;
    }

    public void setTipePasienId(Integer tipePasienId) {
        this.tipePasienId = tipePasienId;
    }

    public String getPrioritas() {
        return prioritas;
    }

    public void setPrioritas(String prioritas) {
        this.prioritas = prioritas;
    }
}
