package com.vone.simrs.master.staff;

import java.util.List;

/**
 * Baris data staff (SCM0031). Mengikuti entity legacy {@code MsStaff}
 * (tabel ms_staff) yang digabung dengan relasi {@code MsStaffInUnit}
 * (tabel ms_staff_in_unit) untuk daftar sub divisi/unit.
 */
public class StaffRowResponse {

    private final Integer staffId;
    private final String code;
    private final String name;
    private final String address;
    private final String phone;
    private final Integer coaId;
    private final String coaNo;
    private final Double salary;
    private final String hiredDate;
    private final String firedDate;
    private final List<Integer> unitIds;
    private final List<String> unitNames;

    public StaffRowResponse(Integer staffId, String code, String name,
            String address, String phone, Integer coaId, String coaNo,
            Double salary, String hiredDate, String firedDate,
            List<Integer> unitIds, List<String> unitNames) {
        this.staffId = staffId;
        this.code = code;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.coaId = coaId;
        this.coaNo = coaNo;
        this.salary = salary;
        this.hiredDate = hiredDate;
        this.firedDate = firedDate;
        this.unitIds = unitIds;
        this.unitNames = unitNames;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public String getCoaNo() {
        return coaNo;
    }

    public Double getSalary() {
        return salary;
    }

    public String getHiredDate() {
        return hiredDate;
    }

    public String getFiredDate() {
        return firedDate;
    }

    public List<Integer> getUnitIds() {
        return unitIds;
    }

    public List<String> getUnitNames() {
        return unitNames;
    }
}
