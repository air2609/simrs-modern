package com.vone.simrs.master.treatment;

public class LabTreatmentGroupResponse {

    private final Integer groupId;
    private final String code;
    private final String name;

    public LabTreatmentGroupResponse(Integer groupId, String code, String name) {
        this.groupId = groupId;
        this.code = code;
        this.name = name;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
