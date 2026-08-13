package com.vone.simrs.master.screen;

public class UnitOptionResponse {

    private final Integer unitId;
    private final String unitCode;
    private final String unitName;

    public UnitOptionResponse(Integer unitId, String unitCode, String unitName) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }
}
