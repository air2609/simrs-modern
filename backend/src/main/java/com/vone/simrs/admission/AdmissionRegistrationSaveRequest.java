package com.vone.simrs.admission;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AdmissionRegistrationSaveRequest {

    private String existingMrCode;

    @NotBlank
    private String patientName;

    @NotBlank
    private String gender;

    @NotBlank
    private String birthDate;

    @NotBlank
    private String nik;

    @NotBlank
    private String mainAddress;

    @NotNull
    private Integer unitId;

    @NotNull
    private Integer doctorStaffId;

    private Integer patientTypeId;
    private String mainPhone;
    private String mainRt;
    private String mainRw;
    private String altAddress;
    private String altPhone;
    private String altRt;
    private String altRw;
    private String maritalStatus;
    private String nationality;
    private String religion;
    private String education;
    private String jobType;
    private String priority;
    private String etnis;
    private String language;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String subdistrictCode;

    public String getExistingMrCode() {
        return existingMrCode;
    }

    public void setExistingMrCode(String existingMrCode) {
        this.existingMrCode = existingMrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(String mainAddress) {
        this.mainAddress = mainAddress;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public Integer getDoctorStaffId() {
        return doctorStaffId;
    }

    public void setDoctorStaffId(Integer doctorStaffId) {
        this.doctorStaffId = doctorStaffId;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public void setPatientTypeId(Integer patientTypeId) {
        this.patientTypeId = patientTypeId;
    }

    public String getMainPhone() {
        return mainPhone;
    }

    public void setMainPhone(String mainPhone) {
        this.mainPhone = mainPhone;
    }

    public String getMainRt() {
        return mainRt;
    }

    public void setMainRt(String mainRt) {
        this.mainRt = mainRt;
    }

    public String getMainRw() {
        return mainRw;
    }

    public void setMainRw(String mainRw) {
        this.mainRw = mainRw;
    }

    public String getAltAddress() {
        return altAddress;
    }

    public void setAltAddress(String altAddress) {
        this.altAddress = altAddress;
    }

    public String getAltPhone() {
        return altPhone;
    }

    public void setAltPhone(String altPhone) {
        this.altPhone = altPhone;
    }

    public String getAltRt() {
        return altRt;
    }

    public void setAltRt(String altRt) {
        this.altRt = altRt;
    }

    public String getAltRw() {
        return altRw;
    }

    public void setAltRw(String altRw) {
        this.altRw = altRw;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getEtnis() {
        return etnis;
    }

    public void setEtnis(String etnis) {
        this.etnis = etnis;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(String districtCode) {
        this.districtCode = districtCode;
    }

    public String getSubdistrictCode() {
        return subdistrictCode;
    }

    public void setSubdistrictCode(String subdistrictCode) {
        this.subdistrictCode = subdistrictCode;
    }
}
