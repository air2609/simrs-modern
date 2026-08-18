package com.vone.simrs.ward;

import java.util.List;

/**
 * Request simpan/ubah nota bangsal.
 */
public class WardNoteSaveRequest {

    private String mrCode;
    private Integer unitId;
    private Integer doctorId;
    private Integer escortId;
    private List<WardLineRequest> lines;

    public String getMrCode() {
        return mrCode;
    }

    public void setMrCode(String mrCode) {
        this.mrCode = mrCode;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public void setEscortId(Integer escortId) {
        this.escortId = escortId;
    }

    public List<WardLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<WardLineRequest> lines) {
        this.lines = lines;
    }
}
