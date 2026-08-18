package com.vone.simrs.ward;

/**
 * Satu baris pemakaian inventory pasien (itemId + qty keluar).
 */
public class WardPatientInventoryLineRequest {

    private Integer itemId;
    private Integer qtyOut;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getQtyOut() {
        return qtyOut;
    }

    public void setQtyOut(Integer qtyOut) {
        this.qtyOut = qtyOut;
    }
}
