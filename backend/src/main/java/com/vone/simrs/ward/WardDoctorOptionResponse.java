package com.vone.simrs.ward;

/**
 * Dokter untuk bandbox DOKTER UTAMA. Migrasi dari legacy
 * {@code DoctorController.searchDoctor()} (grup medis DOKTER).
 */
public class WardDoctorOptionResponse {

    private final Integer staffId;
    private final String code;
    private final String name;

    public WardDoctorOptionResponse(Integer staffId, String code, String name) {
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
