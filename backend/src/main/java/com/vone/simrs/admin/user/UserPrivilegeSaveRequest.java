package com.vone.simrs.admin.user;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UserPrivilegeSaveRequest {

    @NotBlank
    private String userName;

    @NotNull
    private Integer screenId;

    @NotBlank
    private String accessType;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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
