package com.vone.simrs.laborat;

import java.util.List;

public class LaboratPatientDetailResponse {
    private final String mrCode;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final Integer age;
    private final String address;
    private final String religion;
    private final String patientTypeName;
    private final String doctorName;
    private final String escortName;
    private final Integer patientId;
    private final boolean inpatient;
    private final String tariffClass;
    private final List<LaboratRegistrationOption> registrations;

    public LaboratPatientDetailResponse(String mrCode, String patientName, String gender, String birthDate,
            Integer age, String address, String religion, String patientTypeName,
            String doctorName, String escortName,
            Integer patientId, boolean inpatient, String tariffClass,
            List<LaboratRegistrationOption> registrations) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.age = age;
        this.address = address;
        this.religion = religion;
        this.patientTypeName = patientTypeName;
        this.doctorName = doctorName;
        this.escortName = escortName;
        this.patientId = patientId;
        this.inpatient = inpatient;
        this.tariffClass = tariffClass;
        this.registrations = registrations;
    }

    public String getMrCode() { return mrCode; }
    public String getPatientName() { return patientName; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }
    public Integer getAge() { return age; }
    public String getAddress() { return address; }
    public String getReligion() { return religion; }
    public String getPatientTypeName() { return patientTypeName; }
    public String getDoctorName() { return doctorName; }
    public String getEscortName() { return escortName; }
    public Integer getPatientId() { return patientId; }
    public boolean isInpatient() { return inpatient; }
    public String getTariffClass() { return tariffClass; }
    public List<LaboratRegistrationOption> getRegistrations() { return registrations; }
}

class LaboratRegistrationOption {
    private final Integer registrationId;
    private final String registrationCode;
    private final String unitName;
    private final String registrationDate;

    public LaboratRegistrationOption(Integer registrationId, String registrationCode, String unitName, String registrationDate) {
        this.registrationId = registrationId;
        this.registrationCode = registrationCode;
        this.unitName = unitName;
        this.registrationDate = registrationDate;
    }

    public Integer getRegistrationId() { return registrationId; }
    public String getRegistrationCode() { return registrationCode; }
    public String getUnitName() { return unitName; }
    public String getRegistrationDate() { return registrationDate; }
}
