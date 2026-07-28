package com.vone.simrs.laborat;

public class LaboratNoteLineResponse {
    private final String lineType;
    private final Integer refId;
    private final String code;
    private final String description;
    private final double unitPrice;
    private final double quantity;
    private final double subtotal;
    private final double discountAmount;
    private final String discountType;

    public LaboratNoteLineResponse(String lineType, Integer refId, String code, String description, double unitPrice,
            double quantity, double subtotal, double discountAmount, String discountType) {
        this.lineType = lineType;
        this.refId = refId;
        this.code = code;
        this.description = description;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.discountType = discountType;
    }

    public String getLineType() { return lineType; }
    public Integer getRefId() { return refId; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public double getUnitPrice() { return unitPrice; }
    public double getQuantity() { return quantity; }
    public double getSubtotal() { return subtotal; }
    public double getDiscountAmount() { return discountAmount; }
    public String getDiscountType() { return discountType; }
}
