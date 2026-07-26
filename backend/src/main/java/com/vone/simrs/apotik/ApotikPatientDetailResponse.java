package com.vone.simrs.apotik;

public class ApotikPatientDetailResponse {

    private final Integer patientId;
    private final Integer medicalRecordId;
    private final String mrCode;
    private final Integer registrationId;
    private final String registrationCode;
    private final Integer patientTypeId;
    private final String patientTypeName;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String address;
    private final boolean inpatient;
    private final String tariffClass;

    public ApotikPatientDetailResponse(
            Integer patientId, Integer medicalRecordId, String mrCode,
            Integer registrationId, String registrationCode,
            Integer patientTypeId, String patientTypeName, String patientName, String gender,
            String birthDate, String address, boolean inpatient, String tariffClass) {
        this.patientId = patientId;
        this.medicalRecordId = medicalRecordId;
        this.mrCode = mrCode;
        this.registrationId = registrationId;
        this.registrationCode = registrationCode;
        this.patientTypeId = patientTypeId;
        this.patientTypeName = patientTypeName;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.inpatient = inpatient;
        this.tariffClass = tariffClass;
    }

    public Integer getPatientId() { return patientId; }
    public Integer getMedicalRecordId() { return medicalRecordId; }
    public String getMrCode() { return mrCode; }
    public Integer getRegistrationId() { return registrationId; }
    public String getRegistrationCode() { return registrationCode; }
    public Integer getPatientTypeId() { return patientTypeId; }
    public String getPatientTypeName() { return patientTypeName; }
    public String getPatientName() { return patientName; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }
    public String getAddress() { return address; }
    public boolean isInpatient() { return inpatient; }
    public String getTariffClass() { return tariffClass; }
}
