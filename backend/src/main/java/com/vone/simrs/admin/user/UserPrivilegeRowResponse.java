package com.vone.simrs.admin.user;

public class UserPrivilegeRowResponse {

    private final Integer screenId;
    private final String screenCode;
    private final String screenName;
    private final String accessType;

    public UserPrivilegeRowResponse(Integer screenId, String screenCode, String screenName, String accessType) {
        this.screenId = screenId;
        this.screenCode = screenCode;
        this.screenName = screenName;
        this.accessType = accessType;
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

    public String getAccessType() {
        return accessType;
    }
}
