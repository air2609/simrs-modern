package com.vone.simrs.apotik;

public class ApotikRegisteredPatientResponse {

    private final Integer medicalRecordId;
    private final String mrCode;
    private final String patientName;
    private final String address;

    public ApotikRegisteredPatientResponse(Integer medicalRecordId, String mrCode, String patientName, String address) {
        this.medicalRecordId = medicalRecordId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.address = address;
    }

    public Integer getMedicalRecordId() {
        return medicalRecordId;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getAddress() {
        return address;
    }
}
