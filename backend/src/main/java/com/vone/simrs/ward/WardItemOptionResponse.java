package com.vone.simrs.ward;

/**
 * Obat/bahan medis (O-BM) untuk dialog TAMBAH O-BM.
 */
public class WardItemOptionResponse {

    private final Integer itemId;
    private final String code;
    private final String name;
    private final String unit;
    private final Double price;
    private final Double stock;

    public WardItemOptionResponse(Integer itemId, String code, String name, String unit,
            Double price, Double stock) {
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.price = price;
        this.stock = stock;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public Double getPrice() {
        return price;
    }

    public Double getStock() {
        return stock;
    }
}
