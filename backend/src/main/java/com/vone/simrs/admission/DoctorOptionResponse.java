package com.vone.simrs.admission;

public class DoctorOptionResponse {

    private final Integer staffId;
    private final String staffCode;
    private final String staffName;

    public DoctorOptionResponse(Integer staffId, String staffCode, String staffName) {
        this.staffId = staffId;
        this.staffCode = staffCode;
        this.staffName = staffName;
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
}
