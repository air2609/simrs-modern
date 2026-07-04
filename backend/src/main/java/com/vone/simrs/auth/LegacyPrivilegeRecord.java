package com.vone.simrs.auth;

public class LegacyPrivilegeRecord {

    private final int priority;
    private final Integer moduleId;
    private final String moduleCode;
    private final String moduleName;
    private final Integer screenId;
    private final String screenCode;
    private final String screenName;
    private final String accessType;

    public LegacyPrivilegeRecord(
        int priority,
        Integer moduleId,
        String moduleCode,
        String moduleName,
        Integer screenId,
        String screenCode,
        String screenName,
        String accessType
    ) {
        this.priority = priority;
        this.moduleId = moduleId;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.screenId = screenId;
        this.screenCode = screenCode;
        this.screenName = screenName;
        this.accessType = accessType;
    }

    public int getPriority() {
        return priority;
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
