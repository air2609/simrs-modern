package com.vone.simrs.admission;

public class AdmissionPatientDetailResponse {

    private final Integer patientId;
    private final Integer medicalRecordId;
    private final String mrCode;
    private final String activeRegistrationCode;
    private final String ihsNumber;
    private final Integer patientTypeId;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String nik;
    private final String mainAddress;
    private final String mainPhone;
    private final String mainRt;
    private final String mainRw;
    private final String altAddress;
    private final String altPhone;
    private final String altRt;
    private final String altRw;
    private final String maritalStatus;
    private final String nationality;
    private final String religion;
    private final String education;
    private final String jobType;
    private final String priority;
    private final String etnis;
    private final String language;
    private final String provinceCode;
    private final String cityCode;
    private final String districtCode;
    private final String subdistrictCode;

    public AdmissionPatientDetailResponse(
        Integer patientId,
        Integer medicalRecordId,
        String mrCode,
        String activeRegistrationCode,
        String ihsNumber,
        Integer patientTypeId,
        String patientName,
        String gender,
        String birthDate,
        String nik,
        String mainAddress,
        String mainPhone,
        String mainRt,
        String mainRw,
        String altAddress,
        String altPhone,
        String altRt,
        String altRw,
        String maritalStatus,
        String nationality,
        String religion,
        String education,
        String jobType,
        String priority,
        String etnis,
        String language,
        String provinceCode,
        String cityCode,
        String districtCode,
        String subdistrictCode
    ) {
        this.patientId = patientId;
        this.medicalRecordId = medicalRecordId;
        this.mrCode = mrCode;
        this.activeRegistrationCode = activeRegistrationCode;
        this.ihsNumber = ihsNumber;
        this.patientTypeId = patientTypeId;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.nik = nik;
        this.mainAddress = mainAddress;
        this.mainPhone = mainPhone;
        this.mainRt = mainRt;
        this.mainRw = mainRw;
        this.altAddress = altAddress;
        this.altPhone = altPhone;
        this.altRt = altRt;
        this.altRw = altRw;
        this.maritalStatus = maritalStatus;
        this.nationality = nationality;
        this.religion = religion;
        this.education = education;
        this.jobType = jobType;
        this.priority = priority;
        this.etnis = etnis;
        this.language = language;
        this.provinceCode = provinceCode;
        this.cityCode = cityCode;
        this.districtCode = districtCode;
        this.subdistrictCode = subdistrictCode;
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

    public String getActiveRegistrationCode() {
        return activeRegistrationCode;
    }

    public String getIhsNumber() {
        return ihsNumber;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
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

    public String getNik() {
        return nik;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public String getMainPhone() {
        return mainPhone;
    }

    public String getMainRt() {
        return mainRt;
    }

    public String getMainRw() {
        return mainRw;
    }

    public String getAltAddress() {
        return altAddress;
    }

    public String getAltPhone() {
        return altPhone;
    }

    public String getAltRt() {
        return altRt;
    }

    public String getAltRw() {
        return altRw;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public String getNationality() {
        return nationality;
    }

    public String getReligion() {
        return religion;
    }

    public String getEducation() {
        return education;
    }

    public String getJobType() {
        return jobType;
    }

    public String getPriority() {
        return priority;
    }

    public String getEtnis() {
        return etnis;
    }

    public String getLanguage() {
        return language;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public String getSubdistrictCode() {
        return subdistrictCode;
    }
}
