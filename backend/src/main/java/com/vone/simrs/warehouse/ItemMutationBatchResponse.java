package com.vone.simrs.warehouse;

/**
 * Batch inventory untuk dialog DETAIL item (screen SC0121). Migrasi dari legacy
 * {@code WarehouseController.detailClick()} + {@code WarehouseDAO.getTbItemInventory()}.
 */
public class ItemMutationBatchResponse {

    private final Integer batchId;
    private final String code;
    private final String name;
    private final String unit;
    private final Integer stock;

    public ItemMutationBatchResponse(Integer batchId, String code, String name, String unit,
            Integer stock) {
        this.batchId = batchId;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.stock = stock;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getStock() {
        return stock;
    }
}
