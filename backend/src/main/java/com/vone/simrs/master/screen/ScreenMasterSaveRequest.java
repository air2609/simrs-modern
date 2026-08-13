package com.vone.simrs.master.screen;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ScreenMasterSaveRequest {

    @NotBlank
    private String screenCode;

    @NotBlank
    private String screenName;

    @NotNull
    private Integer subsystemId;

    private List<Integer> unitIds;

    public String getScreenCode() {
        return screenCode;
    }

    public void setScreenCode(String screenCode) {
        this.screenCode = screenCode;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public Integer getSubsystemId() {
        return subsystemId;
    }

    public void setSubsystemId(Integer subsystemId) {
        this.subsystemId = subsystemId;
    }

    public List<Integer> getUnitIds() {
        return unitIds;
    }

    public void setUnitIds(List<Integer> unitIds) {
        this.unitIds = unitIds;
    }
}
