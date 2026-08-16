package com.vone.simrs.master.staff;

import com.vone.simrs.master.doctor.CoaOptionResponse;
import com.vone.simrs.master.doctor.UnitOptionResponse;
import java.util.List;

/**
 * Data master untuk form staff (SCM0031): opsi unit (sub divisi) dan opsi
 * COA. Mengikuti pilihan yang ada pada legacy {@code msStaff.zul}.
 */
public class StaffMastersResponse {

    private final List<UnitOptionResponse> unitOptions;
    private final List<CoaOptionResponse> coaOptions;

    public StaffMastersResponse(List<UnitOptionResponse> unitOptions,
            List<CoaOptionResponse> coaOptions) {
        this.unitOptions = unitOptions;
        this.coaOptions = coaOptions;
    }

    public List<UnitOptionResponse> getUnitOptions() {
        return unitOptions;
    }

    public List<CoaOptionResponse> getCoaOptions() {
        return coaOptions;
    }
}
