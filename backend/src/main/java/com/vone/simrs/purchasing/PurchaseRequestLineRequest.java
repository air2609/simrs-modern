package com.vone.simrs.purchasing;

/**
 * Baris item pada request simpan OPP (SC0191).
 * Migrasi dari legacy {@code PORManagerImpl.doSaveAddPORController()}.
 */
public class PurchaseRequestLineRequest {

    private Integer itemId;
    private Short qtyRequested;
    private Integer itemMeasurementId;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Short getQtyRequested() {
        return qtyRequested;
    }

    public void setQtyRequested(Short qtyRequested) {
        this.qtyRequested = qtyRequested;
    }

    public Integer getItemMeasurementId() {
        return itemMeasurementId;
    }

    public void setItemMeasurementId(Integer itemMeasurementId) {
        this.itemMeasurementId = itemMeasurementId;
    }
}