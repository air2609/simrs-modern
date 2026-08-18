package com.vone.simrs.warehouse;

/**
 * Satu baris permintaan item (itemId + jumlah).
 */
public class ItemRequestLineRequest {

    private Integer itemId;
    private Integer qty;

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}
