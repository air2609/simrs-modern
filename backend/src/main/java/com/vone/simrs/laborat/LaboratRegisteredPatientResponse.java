package com.vone.simrs.laborat;

public class LaboratRegisteredPatientResponse {
    private final String mrCode;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String address;
    private final Integer patientId;

    public LaboratRegisteredPatientResponse(String mrCode, String patientName, String gender, String birthDate, String address, Integer patientId) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.patientId = patientId;
    }

    public String getMrCode() { return mrCode; }
    public String getPatientName() { return patientName; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }
    public String getAddress() { return address; }
    public Integer getPatientId() { return patientId; }
}
