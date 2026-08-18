package com.vone.simrs.ward;

/**
 * Satu baris tanggal yang dipilih untuk dibuatkan nota bed.
 */
public class BedNoteRowRequest {

    private Integer bedId;
    private String date;
    private Double harga;

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Double getHarga() {
        return harga;
    }

    public void setHarga(Double harga) {
        this.harga = harga;
    }
}
