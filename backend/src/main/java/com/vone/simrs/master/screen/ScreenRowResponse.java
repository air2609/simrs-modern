package com.vone.simrs.master.screen;

import java.util.List;

public class ScreenRowResponse {

    private final Integer screenId;
    private final String screenCode;
    private final String screenName;
    private final Integer subsystemId;
    private final String subsystemCode;
    private final String subsystemName;
    private final List<Integer> unitIds;

    public ScreenRowResponse(
            Integer screenId,
            String screenCode,
            String screenName,
            Integer subsystemId,
            String subsystemCode,
            String subsystemName,
            List<Integer> unitIds) {
        this.screenId = screenId;
        this.screenCode = screenCode;
        this.screenName = screenName;
        this.subsystemId = subsystemId;
        this.subsystemCode = subsystemCode;
        this.subsystemName = subsystemName;
        this.unitIds = unitIds;
    }

    public Integer getScreenId() {
        return screenId;
    }

    public String getScreenCode() {
        return screenCode;
    }

    public String getScreenName() {
        return screenName;
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

    public List<Integer> getUnitIds() {
        return unitIds;
    }
}
