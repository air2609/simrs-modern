package com.vone.simrs.admin.user;

public class ScreenOptionResponse {

    private final Integer screenId;
    private final String screenCode;
    private final String screenName;

    public ScreenOptionResponse(Integer screenId, String screenCode, String screenName) {
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
