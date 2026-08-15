package com.vone.simrs.master.doctor;

import java.util.List;

/**
 * Data master untuk form dokter (SCM0030): opsi unit, opsi COA, opsi group
 * staff medis, opsi tingkat keahlian, dan opsi status. Mengikuti pilihan yang
 * ada pada legacy {@code msDokter.zul}.
 */
public class DoctorMastersResponse {

    private final List<UnitOptionResponse> unitOptions;
    private final List<CoaOptionResponse> coaOptions;
    private final List<MedicStaffGroupOptionResponse> medicStaffGroupOptions;
    private final List<String> levelOfExpertiseOptions;
    private final List<String> statusOptions;

    public DoctorMastersResponse(List<UnitOptionResponse> unitOptions,
            List<CoaOptionResponse> coaOptions,
            List<MedicStaffGroupOptionResponse> medicStaffGroupOptions,
            List<String> levelOfExpertiseOptions,
            List<String> statusOptions) {
        this.unitOptions = unitOptions;
        this.coaOptions = coaOptions;
        this.medicStaffGroupOptions = medicStaffGroupOptions;
        this.levelOfExpertiseOptions = levelOfExpertiseOptions;
        this.statusOptions = statusOptions;
    }

    public List<UnitOptionResponse> getUnitOptions() {
        return unitOptions;
    }

    public List<CoaOptionResponse> getCoaOptions() {
        return coaOptions;
    }

    public List<MedicStaffGroupOptionResponse> getMedicStaffGroupOptions() {
        return medicStaffGroupOptions;
    }

    public List<String> getLevelOfExpertiseOptions() {
        return levelOfExpertiseOptions;
    }

    public List<String> getStatusOptions() {
        return statusOptions;
    }
}
