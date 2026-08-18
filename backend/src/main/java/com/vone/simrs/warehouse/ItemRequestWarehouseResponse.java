package com.vone.simrs.warehouse;

/**
 * Gudang (sumber/tujuan) untuk screen SC0174 (PERMINTAAN O-BM).
 */
public class ItemRequestWarehouseResponse {

    private final Integer warehouseId;
    private final String code;
    private final String name;

    public ItemRequestWarehouseResponse(Integer warehouseId, String code, String name) {
        this.warehouseId = warehouseId;
        this.code = code;
        this.name = name;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
