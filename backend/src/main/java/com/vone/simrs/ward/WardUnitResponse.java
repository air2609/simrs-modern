package com.vone.simrs.ward;

/**
 * Unit lokasi transaksi bangsal. Migrasi dari legacy
 * {@code UserManagerImpl.getUnitUser()} (unit tempat user bertugas).
 */
public class WardUnitResponse {

    private final Integer unitId;
    private final String code;
    private final String name;
    private final Integer warehouseId;

    public WardUnitResponse(Integer unitId, String code, String name, Integer warehouseId) {
        this.unitId = unitId;
        this.code = code;
        this.name = name;
        this.warehouseId = warehouseId;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }
}
