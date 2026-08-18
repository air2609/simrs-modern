package com.vone.simrs.cashier;

/**
 * Baris nota untuk tabel transaksi kasir.
 */
public class CashierNoteLineResponse {

    private final Integer noteId;
    private final String noteNo;
    private final String code;
    private final String name;
    private final Double qty;
    private final String unit;
    private final Double price;
    private final Double discount;
    private final Double subtotal;

    public CashierNoteLineResponse(Integer noteId, String noteNo, String code, String name,
            Double qty, String unit, Double price, Double discount, Double subtotal) {
        this.noteId = noteId;
        this.noteNo = noteNo;
        this.code = code;
        this.name = name;
        this.qty = qty;
        this.unit = unit;
        this.price = price;
        this.discount = discount;
        this.subtotal = subtotal;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNo() {
        return noteNo;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Double getQty() {
        return qty;
    }

    public String getUnit() {
        return unit;
    }

    public Double getPrice() {
        return price;
    }

    public Double getDiscount() {
        return discount;
    }

    public Double getSubtotal() {
        return subtotal;
    }
}
