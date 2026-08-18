package com.vone.simrs.warehouse;

/**
 * Item hasil pencarian di gudang tujuan. Migrasi dari legacy
 * {@code ItemDAO.searchItemByWarehouese()} + {@code WarehouseDAO.getQtyAvail()}.
 */
public class ItemRequestItemResponse {

    private final Integer itemId;
    private final String code;
    private final String name;
    private final String unit;
    private final Integer stock;

    public ItemRequestItemResponse(Integer itemId, String code, String name, String unit,
            Integer stock) {
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.unit = unit;
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

    public Integer getStock() {
        return stock;
    }
}
