package com.vone.simrs.master.treatment;

public class LabTreatmentResponse {

    private final Integer treatmentId;
    private final String code;
    private final String name;
    private final Integer groupId;

    public LabTreatmentResponse(Integer treatmentId, String code, String name, Integer groupId) {
        this.treatmentId = treatmentId;
        this.code = code;
        this.name = name;
        this.groupId = groupId;
    }

    public Integer getTreatmentId() {
        return treatmentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getGroupId() {
        return groupId;
    }
}
