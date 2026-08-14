package com.vone.simrs.master.warehouse;

/**
 * Opsi gudang untuk dropdown "GUDANG UTAMA" (SCM0035). Mengikuti
 * {@code WarehouseController.getWarehouseList()} pada tabel ms_warehouse.
 */
public class WarehouseOptionResponse {

    private final Integer id;
    private final String whouseCode;
    private final String whouseName;

    public WarehouseOptionResponse(Integer id, String whouseCode, String whouseName) {
        this.id = id;
        this.whouseCode = whouseCode;
        this.whouseName = whouseName;
    }

    public Integer getId() {
        return id;
    }

    public String getWhouseCode() {
        return whouseCode;
    }

    public String getWhouseName() {
        return whouseName;
    }
}
