package com.vone.simrs.admin.group;

public class GroupRowResponse {

    private final Integer groupId;
    private final String groupCode;
    private final String groupName;

    public GroupRowResponse(Integer groupId, String groupCode, String groupName) {
        this.groupId = groupId;
        this.groupCode = groupCode;
        this.groupName = groupName;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupName() {
        return groupName;
    }
}
