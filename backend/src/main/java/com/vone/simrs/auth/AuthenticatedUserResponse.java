package com.vone.simrs.auth;

import java.util.List;

public class AuthenticatedUserResponse {

    private final Integer userId;
    private final String username;
    private final String fullName;
    private final Integer groupId;
    private final Integer branchId;
    private final List<AuthModuleResponse> modules;

    public AuthenticatedUserResponse(
        Integer userId,
        String username,
        String fullName,
        Integer groupId,
        Integer branchId,
        List<AuthModuleResponse> modules
    ) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.groupId = groupId;
        this.branchId = branchId;
        this.modules = modules;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public List<AuthModuleResponse> getModules() {
        return modules;
    }
}
