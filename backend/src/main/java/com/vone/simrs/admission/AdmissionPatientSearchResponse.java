package com.vone.simrs.admission;

public class AdmissionPatientSearchResponse {

    private final Integer patientId;
    private final Integer medicalRecordId;
    private final String mrCode;
    private final String patientName;
    private final String nik;
    private final String birthDate;
    private final String address;

    public AdmissionPatientSearchResponse(
        Integer patientId,
        Integer medicalRecordId,
        String mrCode,
        String patientName,
        String nik,
        String birthDate,
        String address
    ) {
        this.patientId = patientId;
        this.medicalRecordId = medicalRecordId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.nik = nik;
        this.birthDate = birthDate;
        this.address = address;
    }

    public Integer getPatientId() {
        return patientId;
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

    public String getNik() {
        return nik;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }
}
