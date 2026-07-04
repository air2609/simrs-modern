package com.vone.simrs.admission;

public class PatientTypeOptionResponse {

    private final Integer patientTypeId;
    private final String patientTypeCode;
    private final String patientTypeDescription;

    public PatientTypeOptionResponse(Integer patientTypeId, String patientTypeCode, String patientTypeDescription) {
        this.patientTypeId = patientTypeId;
        this.patientTypeCode = patientTypeCode;
        this.patientTypeDescription = patientTypeDescription;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientTypeCode() {
        return patientTypeCode;
    }

    public String getPatientTypeDescription() {
        return patientTypeDescription;
    }
}
