package com.vone.simrs.purchasing;

/**
 * Hasil pencarian item pada dialog TAMBAH ITEM (SC0191). Migrasi dari legacy
 * {@code ItemDAO.serachItemUnderBuffer()} + {@code ItemPicker.search()}.
 */
public class PurchaseRequestAddItemResponse {

    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final String itemGroupCode;
    private final Integer stock;
    private final Integer bufferLimit;
    private final Integer maxOrder;
    private final String measurementCode;
    private final Integer measurementId;

    public PurchaseRequestAddItemResponse(Integer itemId, String itemCode, String itemName,
            String itemGroupCode, Integer stock, Integer bufferLimit, Integer maxOrder,
            String measurementCode, Integer measurementId) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemGroupCode = itemGroupCode;
        this.stock = stock;
        this.bufferLimit = bufferLimit;
        this.maxOrder = maxOrder;
        this.measurementCode = measurementCode;
        this.measurementId = measurementId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemGroupCode() {
        return itemGroupCode;
    }

    public Integer getStock() {
        return stock;
    }

    public Integer getBufferLimit() {
        return bufferLimit;
    }

    public Integer getMaxOrder() {
        return maxOrder;
    }

    public String getMeasurementCode() {
        return measurementCode;
    }

    public Integer getMeasurementId() {
        return measurementId;
    }
}