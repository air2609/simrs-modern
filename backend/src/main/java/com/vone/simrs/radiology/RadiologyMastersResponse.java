package com.vone.simrs.radiology;

import com.vone.simrs.physiotherapy.PhysiotherapyEscortResponse;
import com.vone.simrs.physiotherapy.PhysiotherapyPatientTypeResponse;
import com.vone.simrs.ward.WardUnitResponse;
import java.util.List;

/**
 * Masters screen SC0051: unit lokasi transaksi + tipe pasien + tipe pembawa.
 */
public class RadiologyMastersResponse {

    private final List<WardUnitResponse> units;
    private final List<PhysiotherapyPatientTypeResponse> patientTypes;
    private final List<PhysiotherapyEscortResponse> escorts;

    public RadiologyMastersResponse(List<WardUnitResponse> units,
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
