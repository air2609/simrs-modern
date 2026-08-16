package com.vone.simrs.master.itemsellingprice;

/**
 * Request simpan/ubah harga jual item (SCM0041). Mengikuti form legacy
 * {@code msItemSellingPrice.zul} + {@code ItemSellingPriceController}.
 */
public class ItemSellingPriceSaveRequest {

    private Integer id;
    private Integer itemId;
    private Integer tclassId;
    private Double sellingPrice;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getTclassId() {
        return tclassId;
    }

    public void setTclassId(Integer tclassId) {
        this.tclassId = tclassId;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }
}
