package com.vone.simrs.purchasing;

/**
 * Opsi unit untuk dropdown LOKASI TRANSAKSI (SC0191).
 * Migrasi dari legacy {@code PORController.init()} yang mengisi dropdown
 * location dari {@code getUserInfoBean().getMsUnitByScreenCode(...)}.
 */
public class PurchaseRequestUnitResponse {

    private final Integer unitId;
    private final String unitCode;
    private final String unitName;
    private final Integer warehouseId;

    public PurchaseRequestUnitResponse(Integer unitId, String unitCode, String unitName,
            Integer warehouseId) {
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