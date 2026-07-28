package com.vone.simrs.laborat;

public class LaboratItemOptionResponse {
    private final Integer itemId;
    private final String code;
    private final String name;
    private final double price;

    public LaboratItemOptionResponse(Integer itemId, String code, String name, double price) {
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.price = price;
    }

    public Integer getItemId() { return itemId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public double getPrice() { return price; }
}
