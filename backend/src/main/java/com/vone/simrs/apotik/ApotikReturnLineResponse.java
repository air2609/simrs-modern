package com.vone.simrs.apotik;

public class ApotikReturnLineResponse {

    private final Integer detailId;
    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final String unitName;
    private final double originalQuantity;
    private final double returnedQuantity;
    private final double unitPrice;
    private final double value;

    public ApotikReturnLineResponse(
            Integer detailId, Integer itemId, String itemCode,
            String itemName, String unitName, double originalQuantity,
            double returnedQuantity, double unitPrice, double value) {
        this.detailId = detailId;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unitName = unitName;
        this.originalQuantity = originalQuantity;
        this.returnedQuantity = returnedQuantity;
        this.unitPrice = unitPrice;
        this.value = value;
    }

    public Integer getDetailId() { return detailId; }
    public Integer getItemId() { return itemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getUnitName() { return unitName; }
    public double getOriginalQuantity() { return originalQuantity; }
    public double getReturnedQuantity() { return returnedQuantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getValue() { return value; }
}
