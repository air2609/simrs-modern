package com.vone.simrs.admin.group;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class GroupPrivilegeSaveRequest {

    @NotBlank
    private String groupCode;

    @NotNull
    private Integer screenId;

    @NotBlank
    private String accessType;

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public Integer getScreenId() {
        return screenId;
    }

    public void setScreenId(Integer screenId) {
        this.screenId = screenId;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }
}
