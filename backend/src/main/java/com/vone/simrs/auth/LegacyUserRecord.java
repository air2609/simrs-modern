package com.vone.simrs.auth;

public class LegacyUserRecord {

    private final Integer userId;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final Integer groupId;
    private final Integer branchId;

    public LegacyUserRecord(
        Integer userId,
        String username,
        String passwordHash,
        String fullName,
        Integer groupId,
        Integer branchId
    ) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.groupId = groupId;
        this.branchId = branchId;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
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
}
