package com.vone.simrs.laborat;

public class LaboratPatientTypeResponse {
    private final Integer patientTypeId;
    private final String code;
    private final String name;

    public LaboratPatientTypeResponse(Integer patientTypeId, String code, String name) {
        this.patientTypeId = patientTypeId;
        this.code = code;
        this.name = name;
    }

    public Integer getPatientTypeId() { return patientTypeId; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
