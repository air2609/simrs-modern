package com.vone.simrs.apotik;

import java.util.List;

public class ApotikNoteLineResponse {

    private final String lineType;
    private final Integer lineId;
    private final Integer originalId;
    private final String code;
    private final String description;
    private final double quantity;
    private final String unitName;
    private final double unitPrice;
    private final String discountType;
    private final double discountValue;
    private final double subtotal;
    private final String instruction;
    private final List<ApotikCompoundComponentResponse> components;

    public ApotikNoteLineResponse(
            String lineType, Integer lineId, Integer originalId,
            String code, String description, double quantity,
            String unitName, double unitPrice, String discountType,
            double discountValue, double subtotal, String instruction,
            List<ApotikCompoundComponentResponse> components) {
        this.lineType = lineType;
        this.lineId = lineId;
        this.originalId = originalId;
        this.code = code;
        this.description = description;
        this.quantity = quantity;
        this.unitName = unitName;
        this.unitPrice = unitPrice;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.subtotal = subtotal;
        this.instruction = instruction;
        this.components = components;
    }

    public String getLineType() { return lineType; }
    public Integer getLineId() { return lineId; }
    public Integer getOriginalId() { return originalId; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public double getQuantity() { return quantity; }
    public String getUnitName() { return unitName; }
    public double getUnitPrice() { return unitPrice; }
    public String getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public double getSubtotal() { return subtotal; }
    public String getInstruction() { return instruction; }
    public List<ApotikCompoundComponentResponse> getComponents() { return components; }
}
