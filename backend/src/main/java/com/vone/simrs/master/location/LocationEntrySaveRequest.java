package com.vone.simrs.master.location;

import javax.validation.constraints.NotBlank;

public class LocationEntrySaveRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private Integer parentId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }
}
