package com.vone.simrs.emergency;

/**
 * Detail pasien + registrasi terakhir. Migrasi dari legacy
 * {@code EmergencyManagerImpl.getPatientDetil()} yang mengisi data pasien dan
 * dokter utama dari {@code regDao.getLastRegistrationByMrId()}.
 */
public class EmergencyPatientDetailResponse {

    private final Integer mrId;
    private final String mrCode;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String age;
    private final String address;
    private final Integer patientTypeId;
    private final Integer registrationId;
    private final String registrationNumber;
    private final Integer doctorId;
    private final String doctorName;

    public EmergencyPatientDetailResponse(Integer mrId, String mrCode, String patientName,
            String gender, String birthDate, String age, String address, Integer patientTypeId,
            Integer registrationId, String registrationNumber, Integer doctorId, String doctorName) {
        this.mrId = mrId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.age = age;
        this.address = address;
        this.patientTypeId = patientTypeId;
        this.registrationId = registrationId;
        this.registrationNumber = registrationNumber;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
    }

    public Integer getMrId() {
        return mrId;
    }

    public String getMrCode() {
        return mrCode;
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

    public Integer getRegistrationId() {
        return registrationId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }
}
