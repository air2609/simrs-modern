package com.vone.simrs.warehouse;

/**
 * Satu baris pengiriman item (batch + jumlah) pada tombol KIRIM screen SC0121.
 */
public class ItemMutationLineRequest {

    private Integer irId;
    private Integer batchId;
    private Integer qty;

    public Integer getIrId() {
        return irId;
    }

    public void setIrId(Integer irId) {
        this.irId = irId;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}
