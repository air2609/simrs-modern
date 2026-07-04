package com.vone.simrs.system;

import com.vone.simrs.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private final String applicationName;
    private final int legacyUiPages;
    private final int legacyHibernateMappings;
    private final int legacyUiControllers;

    public SystemInfoController(
        @Value("${spring.application.name}") String applicationName,
        @Value("${app.legacy.ui-pages}") int legacyUiPages,
        @Value("${app.legacy.hibernate-mappings}") int legacyHibernateMappings,
        @Value("${app.legacy.ui-controllers}") int legacyUiControllers
    ) {
        this.applicationName = applicationName;
        this.legacyUiPages = legacyUiPages;
        this.legacyHibernateMappings = legacyHibernateMappings;
        this.legacyUiControllers = legacyUiControllers;
    }

    @GetMapping("/info")
    public ApiResponse<SystemInfoResponse> info() {
        return ApiResponse.ok(new SystemInfoResponse(
            applicationName,
            "Spring Boot 2.7 + Java 8",
            "Vue 3 + Vite",
            "Database-first against existing PostgreSQL schema",
            legacyUiPages,
            legacyHibernateMappings,
            legacyUiControllers
        ));
    }
}
