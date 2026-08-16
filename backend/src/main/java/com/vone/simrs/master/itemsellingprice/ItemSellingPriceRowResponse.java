package com.vone.simrs.master.itemsellingprice;

/**
 * Baris data harga jual item (SCM0041). Mengikuti entity legacy
 * {@code MsItemSellingPrice} (tabel ms_item_selling_price) yang digabung
 * dengan ms_item dan ms_treatment_class.
 */
public class ItemSellingPriceRowResponse {

    private final Integer id;
    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final Integer tclassId;
    private final String tclassDesc;
    private final Double sellingPrice;

    public ItemSellingPriceRowResponse(Integer id, Integer itemId, String itemCode,
            String itemName, Integer tclassId, String tclassDesc, Double sellingPrice) {
        this.id = id;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.tclassId = tclassId;
        this.tclassDesc = tclassDesc;
        this.sellingPrice = sellingPrice;
    }

    public Integer getId() {
        return id;
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

    public Integer getTclassId() {
        return tclassId;
    }

    public String getTclassDesc() {
        return tclassDesc;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }
}
