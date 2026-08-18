package com.vone.simrs.warehouse;

/**
 * Baris item mutation (persetujuan penerimaan O-BM).
 */
public class ItemApprovalRowResponse {

    private final Integer mutationId;
    private final Integer irId;
    private final Integer batchId;
    private final Integer itemId;
    private final String code;
    private final String name;
    private final String unit;
    private final Integer qty;

    public ItemApprovalRowResponse(Integer mutationId, Integer irId, Integer batchId,
            Integer itemId, String code, String name, String unit, Integer qty) {
        this.mutationId = mutationId;
        this.irId = irId;
        this.batchId = batchId;
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.qty = qty;
    }

    public Integer getMutationId() {
        return mutationId;
    }

    public Integer getIrId() {
        return irId;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public Integer getItemId() {
        return itemId;
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

    public Integer getQty() {
        return qty;
    }
}
