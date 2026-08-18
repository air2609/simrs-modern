package com.vone.simrs.physiotherapy;

import com.vone.simrs.ward.WardUnitResponse;
import java.util.List;

/**
 * Masters screen SC0141: unit lokasi transaksi + tipe pasien + tipe pembawa.
 */
public class PhysiotherapyMastersResponse {

    private final List<WardUnitResponse> units;
    private final List<PhysiotherapyPatientTypeResponse> patientTypes;
    private final List<PhysiotherapyEscortResponse> escorts;

    public PhysiotherapyMastersResponse(List<WardUnitResponse> units,
            List<PhysiotherapyPatientTypeResponse> patientTypes,
            List<PhysiotherapyEscortResponse> escorts) {
        this.units = units;
        this.patientTypes = patientTypes;
        this.escorts = escorts;
    }

    public List<WardUnitResponse> getUnits() {
        return units;
    }

    public List<PhysiotherapyPatientTypeResponse> getPatientTypes() {
        return patientTypes;
    }

    public List<PhysiotherapyEscortResponse> getEscorts() {
        return escorts;
    }
}
