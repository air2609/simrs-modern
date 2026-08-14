package com.vone.simrs.admin.group;

public class GroupScreenOptionResponse {

    private final Integer screenId;
    private final String screenCode;
    private final String screenName;

    public GroupScreenOptionResponse(Integer screenId, String screenCode, String screenName) {
        this.screenId = screenId;
        this.screenCode = screenCode;
        this.screenName = screenName;
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
}
