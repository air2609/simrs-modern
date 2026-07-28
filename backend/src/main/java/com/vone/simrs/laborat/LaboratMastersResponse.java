package com.vone.simrs.laborat;

import java.util.List;

public class LaboratMastersResponse {

    private final List<LaboratUnitResponse> units;
    private final List<LaboratPatientTypeResponse> patientTypes;
    private final List<LaboratEscortResponse> escorts;

    public LaboratMastersResponse(List<LaboratUnitResponse> units, List<LaboratPatientTypeResponse> patientTypes, List<LaboratEscortResponse> escorts) {
        this.units = units;
        this.patientTypes = patientTypes;
        this.escorts = escorts;
    }

    public List<LaboratUnitResponse> getUnits() { return units; }
    public List<LaboratPatientTypeResponse> getPatientTypes() { return patientTypes; }
    public List<LaboratEscortResponse> getEscorts() { return escorts; }
}
