package com.vone.simrs.master.staff;

import java.util.List;

/**
 * Request simpan/ubah staff (SCM0031). Mengikuti form legacy
 * {@code msStaff.zul} + {@code StaffController.doSaveAdd/doSaveModify}.
 */
public class StaffSaveRequest {

    private Integer staffId;
    private String code;
    private String name;
    private String address;
    private String phone;
    private Integer coaId;
    private Double salary;
    private String hiredDate;
    private String firedDate;
    private List<Integer> unitIds;

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

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
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

    public List<Integer> getUnitIds() {
        return unitIds;
    }

    public void setUnitIds(List<Integer> unitIds) {
        this.unitIds = unitIds;
    }
}
