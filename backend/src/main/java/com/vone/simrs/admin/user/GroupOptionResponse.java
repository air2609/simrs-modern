package com.vone.simrs.admin.user;

public class GroupOptionResponse {

    private final Integer groupId;
    private final String groupName;
    private final String description;

    public GroupOptionResponse(Integer groupId, String groupName, String description) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }
}
