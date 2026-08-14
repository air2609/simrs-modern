package com.vone.simrs.admin.user;

public class UserRowResponse {

    private final Integer userId;
    private final String userName;
    private final String userFullName;
    private final Integer groupId;
    private final String groupName;
    private final Integer staffId;
    private final String staffCode;
    private final Integer branchId;
    private final String branchName;

    public UserRowResponse(Integer userId, String userName, String userFullName,
            Integer groupId, String groupName, Integer staffId, String staffCode,
            Integer branchId, String branchName) {
        this.userId = userId;
        this.userName = userName;
        this.userFullName = userFullName;
        this.groupId = groupId;
        this.groupName = groupName;
        this.staffId = staffId;
        this.staffCode = staffCode;
        this.branchId = branchId;
        this.branchName = branchName;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public String getBranchName() {
        return branchName;
    }
}
