package com.vone.simrs.admin.user;

public class StaffOptionResponse {

    private final Integer staffId;
    private final String staffCode;
    private final String staffName;
    private final String division;

    public StaffOptionResponse(Integer staffId, String staffCode, String staffName, String division) {
        this.staffId = staffId;
        this.staffCode = staffCode;
        this.staffName = staffName;
        this.division = division;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getDivision() {
        return division;
    }
}
