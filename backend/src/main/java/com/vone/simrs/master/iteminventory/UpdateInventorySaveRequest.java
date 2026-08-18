package com.vone.simrs.master.iteminventory;

/**
 * Request simpan update inventory (screen SCM0057). Migrasi dari legacy
 * {@code UpdateInventoryController.doSaveModify()}.
 */
public class UpdateInventorySaveRequest {

    private String itemCode;
    private Double qty;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }
}
