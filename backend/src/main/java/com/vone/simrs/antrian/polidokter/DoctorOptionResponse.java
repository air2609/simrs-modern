package com.vone.simrs.antrian.polidokter;

/**
 * Opsi dokter hasil pencarian (SCM0059). Mengikuti legacy
 * {@code DoctorManagerImpl.searchDoctor} yang menampilkan kode staff, nama
 * staff, dan unit (dari ms_staff_in_unit join ms_unit).
 */
public class DoctorOptionResponse {

    private final Integer staffId;
    private final String staffCode;
    private final String staffName;
    private final String unit;

    public DoctorOptionResponse(Integer staffId, String staffCode, String staffName, String unit) {
        this.staffId = staffId;
        this.staffCode = staffCode;
        this.staffName = staffName;
        this.unit = unit;
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

    public String getUnit() {
        return unit;
    }
}
