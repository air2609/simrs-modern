package com.vone.simrs.admin.user;

public class BranchOptionResponse {

    private final Integer branchId;
    private final String branchName;

    public BranchOptionResponse(Integer branchId, String branchName) {
        this.branchId = branchId;
        this.branchName = branchName;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public String getBranchName() {
        return branchName;
    }
}
