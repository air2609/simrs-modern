package com.vone.simrs.admin.user;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UserSaveRequest {

    @NotBlank
    private String userName;

    @NotBlank
    private String userFullName;

    @NotNull
    private Integer groupId;

    @NotNull
    private Integer staffId;

    private Integer branchId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }
}
