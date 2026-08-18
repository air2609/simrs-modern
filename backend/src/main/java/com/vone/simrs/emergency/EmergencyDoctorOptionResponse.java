package com.vone.simrs.emergency;

/**
 * Dokter (ms_staff role DOKTER). Migrasi dari legacy
 * {@code DoctorManagerImpl.searchDoctor()} + {@code MsDoctorDAO.serarchDoctor()}.
 */
public class EmergencyDoctorOptionResponse {

    private final Integer staffId;
    private final String code;
    private final String name;

    public EmergencyDoctorOptionResponse(Integer staffId, String code, String name) {
        this.staffId = staffId;
        this.code = code;
        this.name = name;
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
}
