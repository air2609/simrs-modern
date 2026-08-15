package com.vone.simrs.master.itemmeasurement;

/**
 * Request simpan/edit satuan item (SCM0040). Mengikuti field yang diisi pada
 * form legacy {@code ItemMeasurementController} (satuan awal, satuan akhir,
 * jumlah pembagi).
 */
public class ItemMeasurementSaveRequest {

    private Integer id;
    private String earlyQuantify;
    private String endQuantify;
    private Short endQty;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEarlyQuantify() {
        return earlyQuantify;
    }

    public void setEarlyQuantify(String earlyQuantify) {
        this.earlyQuantify = earlyQuantify;
    }

    public String getEndQuantify() {
        return endQuantify;
    }

    public void setEndQuantify(String endQuantify) {
        this.endQuantify = endQuantify;
    }

    public Short getEndQty() {
        return endQty;
    }

    public void setEndQty(Short endQty) {
        this.endQty = endQty;
    }
}
