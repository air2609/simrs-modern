package com.vone.simrs.apotik;

import java.util.List;

public class ApotikMastersResponse {

    private final List<ApotikUnitResponse> units;
    private final List<ApotikPatientTypeResponse> patientTypes;
    private final double pajakObatRajal;

    public ApotikMastersResponse(List<ApotikUnitResponse> units, List<ApotikPatientTypeResponse> patientTypes, double pajakObatRajal) {
        this.units = units;
        this.patientTypes = patientTypes;
        this.pajakObatRajal = pajakObatRajal;
    }

    public List<ApotikUnitResponse> getUnits() {
        return units;
    }

    public List<ApotikPatientTypeResponse> getPatientTypes() {
        return patientTypes;
    }

    public double getPajakObatRajal() {
        return pajakObatRajal;
    }
}
