package com.vone.simrs.admission;

import java.util.List;

public class AdmissionRegistrationMastersResponse {

    private final List<UnitOptionResponse> units;
    private final List<PatientTypeOptionResponse> patientTypes;
    private final List<OptionResponse> provinces;

    public AdmissionRegistrationMastersResponse(
        List<UnitOptionResponse> units,
        List<PatientTypeOptionResponse> patientTypes,
        List<OptionResponse> provinces
    ) {
        this.units = units;
        this.patientTypes = patientTypes;
        this.provinces = provinces;
    }

    public List<UnitOptionResponse> getUnits() {
        return units;
    }

    public List<PatientTypeOptionResponse> getPatientTypes() {
        return patientTypes;
    }

    public List<OptionResponse> getProvinces() {
        return provinces;
    }
}
