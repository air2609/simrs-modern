package com.vone.simrs.emergency;

import java.util.List;

/**
 * Request simpan / ubah nota UGD. Migrasi dari legacy
 * {@code EmergencyController.save()} / {@code saveModify()}.
 */
public class EmergencyNoteSaveRequest {

    private String mrCode;
    private Integer patientTypeId;
    private Integer escortId;
    private Integer doctorId;
    private List<EmergencyLineRequest> lines;

    public String getMrCode() {
        return mrCode;
    }

    public void setMrCode(String mrCode) {
        this.mrCode = mrCode;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public void setPatientTypeId(Integer patientTypeId) {
        this.patientTypeId = patientTypeId;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public void setEscortId(Integer escortId) {
        this.escortId = escortId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public List<EmergencyLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<EmergencyLineRequest> lines) {
        this.lines = lines;
    }
}
