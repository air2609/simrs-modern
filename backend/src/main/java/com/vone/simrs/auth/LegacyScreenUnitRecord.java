package com.vone.simrs.auth;

public class LegacyScreenUnitRecord {

    private final Integer screenId;
    private final Integer unitId;
    private final String screenCode;
    private final String unitCode;
    private final String unitName;
    private final Integer warehouseId;

    public LegacyScreenUnitRecord(
        Integer screenId,
        Integer unitId,
        String screenCode,
        String unitCode,
        String unitName,
        Integer warehouseId
    ) {
        this.screenId = screenId;
        this.unitId = unitId;
        this.screenCode = screenCode;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.warehouseId = warehouseId;
    }

    public Integer getScreenId() {
        return screenId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getScreenCode() {
        return screenCode;
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
