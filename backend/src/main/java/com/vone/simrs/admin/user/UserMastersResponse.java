package com.vone.simrs.admin.user;

import java.util.List;

public class UserMastersResponse {

    private final List<GroupOptionResponse> groups;
    private final List<BranchOptionResponse> branches;

    public UserMastersResponse(List<GroupOptionResponse> groups, List<BranchOptionResponse> branches) {
        this.groups = groups;
        this.branches = branches;
    }

    public List<GroupOptionResponse> getGroups() {
        return groups;
    }

    public List<BranchOptionResponse> getBranches() {
        return branches;
    }
}
