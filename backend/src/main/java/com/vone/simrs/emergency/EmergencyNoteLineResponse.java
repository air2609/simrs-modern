package com.vone.simrs.emergency;

/**
 * Baris nota UGD (tindakan / item O-BM / biaya lain-lain). Migrasi dari legacy
 * {@code NoteManagerImpl.getNoteDetil()} yang menampilkan kolom
 * KODE, KETERANGAN, JUMLAH, SATUAN, HARGA, DISKON, SUBTOTAL.
 */
public class EmergencyNoteLineResponse {

    private final String lineType;
    private final Integer referenceId;
    private final String code;
    private final String name;
    private final Double qty;
    private final String unit;
    private final Double price;
    private final String discType;
    private final Double discAmount;
    private final Double subtotal;
    private final Integer doctorId;

    public EmergencyNoteLineResponse(String lineType, Integer referenceId, String code,
            String name, Double qty, String unit, Double price, String discType,
            Double discAmount, Double subtotal, Integer doctorId) {
        this.lineType = lineType;
        this.referenceId = referenceId;
        this.code = code;
        this.name = name;
        this.qty = qty;
        this.unit = unit;
        this.price = price;
        this.discType = discType;
        this.discAmount = discAmount;
        this.subtotal = subtotal;
        this.doctorId = doctorId;
    }

    public String getLineType() {
        return lineType;
    }

    public Integer getReferenceId() {
        return referenceId;
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

    public String getDiscType() {
        return discType;
    }

    public Double getDiscAmount() {
        return discAmount;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public Integer getDoctorId() {
        return doctorId;
    }
}
