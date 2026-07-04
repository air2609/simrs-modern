package com.vone.simrs.auth;

import java.util.List;

public class AuthScreenResponse {

    private final Integer screenId;
    private final String screenCode;
    private final String screenName;
    private final String accessType;
    private final List<AuthUnitResponse> units;

    public AuthScreenResponse(
        Integer screenId,
        String screenCode,
        String screenName,
        String accessType,
        List<AuthUnitResponse> units
    ) {
        this.screenId = screenId;
        this.screenCode = screenCode;
        this.screenName = screenName;
        this.accessType = accessType;
        this.units = units;
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

    public List<AuthUnitResponse> getUnits() {
        return units;
    }
}
