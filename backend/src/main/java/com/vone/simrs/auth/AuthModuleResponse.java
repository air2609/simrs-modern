package com.vone.simrs.auth;

import java.util.List;

public class AuthModuleResponse {

    private final Integer moduleId;
    private final String moduleCode;
    private final String moduleName;
    private final List<AuthScreenResponse> screens;

    public AuthModuleResponse(Integer moduleId, String moduleCode, String moduleName, List<AuthScreenResponse> screens) {
        this.moduleId = moduleId;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.screens = screens;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<AuthScreenResponse> getScreens() {
        return screens;
    }
}
