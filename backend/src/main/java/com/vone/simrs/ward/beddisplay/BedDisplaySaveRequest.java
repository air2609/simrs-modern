package com.vone.simrs.ward.beddisplay;

import javax.validation.constraints.NotNull;

public class BedDisplaySaveRequest {

    @NotNull
    private Integer bedId;

    private boolean shown;

    private String availableStatus;

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
    }

    public boolean isShown() {
        return shown;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }

    public String getAvailableStatus() {
        return availableStatus;
    }

    public void setAvailableStatus(String availableStatus) {
        this.availableStatus = availableStatus;
    }
}
