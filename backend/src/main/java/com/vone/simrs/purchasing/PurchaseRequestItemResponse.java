package com.vone.simrs.purchasing;

/**
 * Baris item pada DAFTAR ORDER PERMINTAAN PEMBELIAN (SC0191).
 * Migrasi dari logika legacy {@code PORManagerImpl.redrawPORController()}
 * yang menampilkan item di bawah buffer beserta stok, buffer limit,
 * max order, satuan, dan jumlah OPP yang masih APPROVED.
 */
public class PurchaseRequestItemResponse {

    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final String itemGroupCode;
    private final Integer stock;
    private final Integer bufferLimit;
    private final Integer maxOrder;
    private final String measurementCode;
    private final Integer openOppCount;
    private final String openOppNumbers;

    public PurchaseRequestItemResponse(Integer itemId, String itemCode, String itemName,
            String itemGroupCode, Integer stock, Integer bufferLimit, Integer maxOrder,
            String measurementCode, Integer openOppCount, String openOppNumbers) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemGroupCode = itemGroupCode;
        this.stock = stock;
        this.bufferLimit = bufferLimit;
        this.maxOrder = maxOrder;
        this.measurementCode = measurementCode;
        this.openOppCount = openOppCount;
        this.openOppNumbers = openOppNumbers;
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

    public Integer getOpenOppCount() {
        return openOppCount;
    }

    public String getOpenOppNumbers() {
        return openOppNumbers;
    }
}