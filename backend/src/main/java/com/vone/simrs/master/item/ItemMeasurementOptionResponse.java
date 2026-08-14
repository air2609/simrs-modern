package com.vone.simrs.master.item;

/**
 * Opsi satuan item (SCM0038). Mengikuti entity legacy {@code MsItemMeasurement}
 * (tabel ms_item_measurement).
 */
public class ItemMeasurementOptionResponse {

    private final Integer id;
    private final String earlyQuantify;
    private final String endQuantify;
    private final short endQty;

    public ItemMeasurementOptionResponse(Integer id, String earlyQuantify, String endQuantify, short endQty) {
        this.id = id;
        this.earlyQuantify = earlyQuantify;
        this.endQuantify = endQuantify;
        this.endQty = endQty;
    }

    public Integer getId() {
        return id;
    }

    public String getEarlyQuantify() {
        return earlyQuantify;
    }

    public String getEndQuantify() {
        return endQuantify;
    }

    public short getEndQty() {
        return endQty;
    }
}
