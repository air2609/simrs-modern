package com.vone.simrs.apotik;

public class ApotikItemOptionResponse {

    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final String unitName;
    private final double price;
    private final double stockQuantity;
    private final Short jasaR;
    private final Short itemType;

    public ApotikItemOptionResponse(
            Integer itemId, String itemCode, String itemName,
            String unitName, double price, double stockQuantity,
            Short jasaR, Short itemType) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unitName = unitName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.jasaR = jasaR;
        this.itemType = itemType;
    }

    public Integer getItemId() { return itemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getUnitName() { return unitName; }
    public double getPrice() { return price; }
    public double getStockQuantity() { return stockQuantity; }
    public Short getJasaR() { return jasaR; }
    public Short getItemType() { return itemType; }
}
