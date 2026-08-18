package com.vone.simrs.warehouse;

/**
 * Baris item pada satu nomor permintaan (tab PERSETUJUAN / HISTORY).
 */
public class ItemRequestRowResponse {

    private final Integer irId;
    private final Integer itemId;
    private final String code;
    private final String name;
    private final String unit;
    private final Integer qtyReq;
    private final Integer qtySent;
    private final Integer sisa;

    public ItemRequestRowResponse(Integer irId, Integer itemId, String code, String name,
            String unit, Integer qtyReq, Integer qtySent, Integer sisa) {
        this.irId = irId;
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.qtyReq = qtyReq;
        this.qtySent = qtySent;
        this.sisa = sisa;
    }

    public Integer getIrId() {
        return irId;
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

    public Integer getQtyReq() {
        return qtyReq;
    }

    public Integer getQtySent() {
        return qtySent;
    }

    public Integer getSisa() {
        return sisa;
    }
}
