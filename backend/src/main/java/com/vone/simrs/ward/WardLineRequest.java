package com.vone.simrs.ward;

/**
 * Satu baris transaksi bangsal (TINDAKAN / ITEM / MISC).
 */
public class WardLineRequest {

    private String lineType;
    private Integer referenceId;
    private Double qty;
    private Double price;
    private String discType;
    private Double discAmount;
    private Integer doctorId;
    private Integer radiograferId;
    private String miscName;

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getDiscType() {
        return discType;
    }

    public void setDiscType(String discType) {
        this.discType = discType;
    }

    public Double getDiscAmount() {
        return discAmount;
    }

    public void setDiscAmount(Double discAmount) {
        this.discAmount = discAmount;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getRadiograferId() {
        return radiograferId;
    }

    public void setRadiograferId(Integer radiograferId) {
        this.radiograferId = radiograferId;
    }

    public String getMiscName() {
        return miscName;
    }

    public void setMiscName(String miscName) {
        this.miscName = miscName;
    }
}
