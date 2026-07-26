package com.vone.simrs.apotik;

public class ApotikCompoundComponentResponse {

    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final String unitName;
    private final double quantity;

    public ApotikCompoundComponentResponse(Integer itemId, String itemCode, String itemName, String unitName, double quantity) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unitName = unitName;
        this.quantity = quantity;
    }

    public Integer getItemId() { return itemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getUnitName() { return unitName; }
    public double getQuantity() { return quantity; }
}
