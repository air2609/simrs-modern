package com.vone.simrs.master.doctor;

import java.util.List;

/**
 * Request simpan/ubah dokter (SCM0030). Mengikuti form legacy
 * {@code msDokter.zul} + {@code DoctorController.doSaveAdd/doSaveModify}.
 */
public class DoctorSaveRequest {

    private Integer id;
    private Integer staffId;
    private String code;
    private String name;
    private String address;
    private String phone;
    private Integer coaId;
    private Integer staffGroup;
    private String levelOfExpertise;
    private String status;
    private Integer outPatientEarnings;
    private String bankAccNo;
    private Integer assistenOf;
    private Integer percentageInPatientWage;
    private Integer docType;
    private Integer flagAntrian;
    private String hiredDate;
    private String firedDate;
    private Double salary;
    private Integer unitId;
    private List<Integer> unitIds;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public void setCoaId(Integer coaId) {
        this.coaId = coaId;
    }

    public Integer getStaffGroup() {
        return staffGroup;
    }

    public void setStaffGroup(Integer staffGroup) {
        this.staffGroup = staffGroup;
    }

    public String getLevelOfExpertise() {
        return levelOfExpertise;
    }

    public void setLevelOfExpertise(String levelOfExpertise) {
        this.levelOfExpertise = levelOfExpertise;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOutPatientEarnings() {
        return outPatientEarnings;
    }

    public void setOutPatientEarnings(Integer outPatientEarnings) {
        this.outPatientEarnings = outPatientEarnings;
    }

    public String getBankAccNo() {
        return bankAccNo;
    }

    public void setBankAccNo(String bankAccNo) {
        this.bankAccNo = bankAccNo;
    }

    public Integer getAssistenOf() {
        return assistenOf;
    }

    public void setAssistenOf(Integer assistenOf) {
        this.assistenOf = assistenOf;
    }

    public Integer getPercentageInPatientWage() {
        return percentageInPatientWage;
    }

    public void setPercentageInPatientWage(Integer percentageInPatientWage) {
        this.percentageInPatientWage = percentageInPatientWage;
    }

    public Integer getDocType() {
        return docType;
    }

    public void setDocType(Integer docType) {
        this.docType = docType;
    }

    public Integer getFlagAntrian() {
        return flagAntrian;
    }

    public void setFlagAntrian(Integer flagAntrian) {
        this.flagAntrian = flagAntrian;
    }

    public String getHiredDate() {
        return hiredDate;
    }

    public void setHiredDate(String hiredDate) {
        this.hiredDate = hiredDate;
    }

    public String getFiredDate() {
        return firedDate;
    }

    public void setFiredDate(String firedDate) {
        this.firedDate = firedDate;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public List<Integer> getUnitIds() {
        return unitIds;
    }

    public void setUnitIds(List<Integer> unitIds) {
        this.unitIds = unitIds;
    }
}
