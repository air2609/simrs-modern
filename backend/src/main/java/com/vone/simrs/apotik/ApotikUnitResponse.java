package com.vone.simrs.apotik;

public class ApotikUnitResponse {

    private final Integer unitId;
    private final String unitCode;
    private final String unitName;
    private final Integer warehouseId;

    public ApotikUnitResponse(Integer unitId, String unitCode, String unitName, Integer warehouseId) {
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.warehouseId = warehouseId;
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

    public Integer getWarehouseId() {
        return warehouseId;
    }
}
