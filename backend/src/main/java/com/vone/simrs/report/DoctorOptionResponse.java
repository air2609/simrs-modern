package com.vone.simrs.report;

/**
 * Hasil pencarian dokter (RPT0013). Migrasi dari legacy
 * {@code MsDoctorDAO.searchDocttor()} (grup dokter = 4).
 */
public class DoctorOptionResponse {

    private final Integer staffId;
    private final String code;
    private final String name;
    private final String units;

    public DoctorOptionResponse(Integer staffId, String code, String name, String units) {
        this.staffId = staffId;
        this.code = code;
        this.name = name;
        this.units = units;
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

    public String getUnits() {
        return units;
    }
}
