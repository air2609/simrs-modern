package com.vone.simrs.system;

public class SystemInfoResponse {

    private final String applicationName;
    private final String backendStack;
    private final String frontendStack;
    private final String databaseStrategy;
    private final int legacyUiPages;
    private final int legacyHibernateMappings;
    private final int legacyUiControllers;

    public SystemInfoResponse(
        String applicationName,
        String backendStack,
        String frontendStack,
        String databaseStrategy,
        int legacyUiPages,
        int legacyHibernateMappings,
        int legacyUiControllers
    ) {
        this.applicationName = applicationName;
        this.backendStack = backendStack;
        this.frontendStack = frontendStack;
        this.databaseStrategy = databaseStrategy;
        this.legacyUiPages = legacyUiPages;
        this.legacyHibernateMappings = legacyHibernateMappings;
        this.legacyUiControllers = legacyUiControllers;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getBackendStack() {
        return backendStack;
    }

    public String getFrontendStack() {
        return frontendStack;
    }

    public String getDatabaseStrategy() {
        return databaseStrategy;
    }

    public int getLegacyUiPages() {
        return legacyUiPages;
    }

    public int getLegacyHibernateMappings() {
        return legacyHibernateMappings;
    }

    public int getLegacyUiControllers() {
        return legacyUiControllers;
    }
}
