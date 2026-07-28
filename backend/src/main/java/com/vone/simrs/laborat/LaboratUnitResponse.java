package com.vone.simrs.laborat;

public class LaboratUnitResponse {
    private final Integer unitId;
    private final String unitCode;
    private final String unitName;

    public LaboratUnitResponse(Integer unitId, String unitCode, String unitName) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
    }

    public Integer getUnitId() { return unitId; }
    public String getUnitCode() { return unitCode; }
    public String getUnitName() { return unitName; }
}
