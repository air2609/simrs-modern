package com.vone.simrs.master.screen;

public class SubsystemOptionResponse {

    private final Integer subsystemId;
    private final String subsystemCode;
    private final String subsystemName;

    public SubsystemOptionResponse(Integer subsystemId, String subsystemCode, String subsystemName) {
        this.subsystemId = subsystemId;
        this.subsystemCode = subsystemCode;
        this.subsystemName = subsystemName;
    }

    public Integer getSubsystemId() {
        return subsystemId;
    }

    public String getSubsystemCode() {
        return subsystemCode;
    }

    public String getSubsystemName() {
        return subsystemName;
    }
}
