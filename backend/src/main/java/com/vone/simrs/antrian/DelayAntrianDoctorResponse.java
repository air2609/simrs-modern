package com.vone.simrs.antrian;

/**
 * Dokter yang memiliki antrian (dari doctor_view). Migrasi dari legacy
 * {@code MsDoctorDAO.getActiveDoctor()}.
 */
public class DelayAntrianDoctorResponse {

    private final Integer staffId;
    private final String name;

    public DelayAntrianDoctorResponse(Integer staffId, String name) {
        this.staffId = staffId;
        this.name = name;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getName() {
        return name;
    }
}
