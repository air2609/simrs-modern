package com.vone.simrs.master.unit;

/**
 * Baris data unit (SCM0024). Mengikuti entity legacy {@code MsUnit}
 * (tabel ms_unit).
 */
public class UnitRowResponse {

    private final Integer id;
    private final String code;
    private final String name;
    private final Integer divisionId;
    private final String divisionName;
    private final Integer unitType;
    private final Integer warehouseId;
    private final String warehouseName;
    private final Integer coaId;
    private final String coaNo;

    public UnitRowResponse(Integer id, String code, String name,
            Integer divisionId, String divisionName, Integer unitType,
            Integer warehouseId, String warehouseName,
            Integer coaId, String coaNo) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.divisionId = divisionId;
        this.divisionName = divisionName;
        this.unitType = unitType;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.coaId = coaId;
        this.coaNo = coaNo;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getDivisionId() {
        return divisionId;
    }

    public String getDivisionName() {
        return divisionName;
    }

    public Integer getUnitType() {
        return unitType;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public String getCoaNo() {
        return coaNo;
    }
}
