package com.vone.simrs.apotik;

public class ApotikPatientTypeResponse {

    private final Integer patientTypeId;
    private final String patientTypeCode;
    private final String patientTypeDesc;

    public ApotikPatientTypeResponse(Integer patientTypeId, String patientTypeCode, String patientTypeDesc) {
        this.patientTypeId = patientTypeId;
        this.patientTypeCode = patientTypeCode;
        this.patientTypeDesc = patientTypeDesc;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getPatientTypeCode() {
        return patientTypeCode;
    }

    public String getPatientTypeDesc() {
        return patientTypeDesc;
    }
}
