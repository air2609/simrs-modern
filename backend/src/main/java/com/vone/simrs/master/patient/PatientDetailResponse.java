package com.vone.simrs.master.patient;

/**
 * Detail pasien utk isi form (SCM0011). Migrasi legacy
 * {@code PatientManagerImpl.getPatientDetil()}.
 */
public class PatientDetailResponse {

    private final String mrCode;
    private final String namaPasien;
    private final String jenisKelamin;
    private final String tglLahir;
    private final String agama;
    private final String wargaNegara;
    private final String statusKawin;
    private final String alamat;
    private final String rt;
    private final String rw;
    private final Integer kelurahanId;
    private final Integer kecamatanId;
    private final Integer kabupatenId;
    private final Integer propinsiId;
    private final String noTelp;
    private final String alamatAlternatif;
    private final String rt1;
    private final String rw1;
    private final String noTelpAlt;
    private final String pendidikan;
    private final String jenisPekerjaan;
    private final Integer tipePasienId;
    private final String prioritas;

    public PatientDetailResponse(String mrCode, String namaPasien, String jenisKelamin,
            String tglLahir, String agama, String wargaNegara, String statusKawin, String alamat,
            String rt, String rw, Integer kelurahanId, Integer kecamatanId, Integer kabupatenId,
            Integer propinsiId, String noTelp, String alamatAlternatif, String rt1, String rw1,
            String noTelpAlt, String pendidikan, String jenisPekerjaan, Integer tipePasienId,
            String prioritas) {
        this.mrCode = mrCode;
        this.namaPasien = namaPasien;
        this.jenisKelamin = jenisKelamin;
        this.tglLahir = tglLahir;
        this.agama = agama;
        this.wargaNegara = wargaNegara;
        this.statusKawin = statusKawin;
        this.alamat = alamat;
        this.rt = rt;
        this.rw = rw;
        this.kelurahanId = kelurahanId;
        this.kecamatanId = kecamatanId;
        this.kabupatenId = kabupatenId;
        this.propinsiId = propinsiId;
        this.noTelp = noTelp;
        this.alamatAlternatif = alamatAlternatif;
        this.rt1 = rt1;
        this.rw1 = rw1;
        this.noTelpAlt = noTelpAlt;
        this.pendidikan = pendidikan;
        this.jenisPekerjaan = jenisPekerjaan;
        this.tipePasienId = tipePasienId;
        this.prioritas = prioritas;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public String getJenisKelamin() {
        return jenisKelamin;
    }

    public String getTglLahir() {
        return tglLahir;
    }

    public String getAgama() {
        return agama;
    }

    public String getWargaNegara() {
        return wargaNegara;
    }

    public String getStatusKawin() {
        return statusKawin;
    }

    public String getAlamat() {
        return alamat;
    }

    public String getRt() {
        return rt;
    }

    public String getRw() {
        return rw;
    }

    public Integer getKelurahanId() {
        return kelurahanId;
    }

    public Integer getKecamatanId() {
        return kecamatanId;
    }

    public Integer getKabupatenId() {
        return kabupatenId;
    }

    public Integer getPropinsiId() {
        return propinsiId;
    }

    public String getNoTelp() {
        return noTelp;
    }

    public String getAlamatAlternatif() {
        return alamatAlternatif;
    }

    public String getRt1() {
        return rt1;
    }

    public String getRw1() {
        return rw1;
    }

    public String getNoTelpAlt() {
        return noTelpAlt;
    }

    public String getPendidikan() {
        return pendidikan;
    }

    public String getJenisPekerjaan() {
        return jenisPekerjaan;
    }

    public Integer getTipePasienId() {
        return tipePasienId;
    }

    public String getPrioritas() {
        return prioritas;
    }
}
