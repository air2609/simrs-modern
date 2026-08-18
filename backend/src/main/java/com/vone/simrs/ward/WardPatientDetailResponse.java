package com.vone.simrs.ward;

/**
 * Detail pasien ranap + registrasi aktif + bed occupancy. Migrasi dari legacy
 * {@code WardTransactionManagerImpl.getRegistrationDetil()}.
 */
public class WardPatientDetailResponse {

    private final Integer mrId;
    private final String mrCode;
    private final Integer registrationId;
    private final String registrationNumber;
    private final Integer patientId;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String age;
    private final String address;
    private final Integer patientTypeId;
    private final String patientTypeName;
    private final Integer doctorId;
    private final String doctorName;
    private final String treatmentClass;
    private final String hall;
    private final String bed;

    public WardPatientDetailResponse(Integer mrId, String mrCode, Integer registrationId,
            String registrationNumber, Integer patientId, String patientName, String gender,
            String birthDate, String age, String address, Integer patientTypeId,
            String patientTypeName, Integer doctorId, String doctorName, String treatmentClass,
            String hall, String bed) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.registrationId = registrationId;
        this.registrationNumber = registrationNumber;
        this.patientId = patientId;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.age = age;
        this.address = address;
        this.patientTypeId = patientTypeId;
        this.patientTypeName = patientTypeName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.treatmentClass = treatmentClass;
        this.hall = hall;
        this.bed = bed;
    }

    public Integer getMrId() {
        return mrId;
    }

    public String getMrCode() {
        return mrCode;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAge() {
        return age;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientTypeName() {
        return patientTypeName;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getTreatmentClass() {
        return treatmentClass;
    }

    public String getHall() {
        return hall;
    }

    public String getBed() {
        return bed;
    }
}
