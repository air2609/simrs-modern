package com.vone.simrs.admission;

/**
 * Request simpan pendaftaran rawat inap.
 */
public class RanapSaveRequest {

    private String mrCode;
    private Integer doctorId;
    private Integer bedId;
    private Integer antriKelasId;

    public String getMrCode() {
        return mrCode;
    }

    public void setMrCode(String mrCode) {
        this.mrCode = mrCode;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
    }

    public Integer getAntriKelasId() {
        return antriKelasId;
    }

    public void setAntriKelasId(Integer antriKelasId) {
        this.antriKelasId = antriKelasId;
    }
}
