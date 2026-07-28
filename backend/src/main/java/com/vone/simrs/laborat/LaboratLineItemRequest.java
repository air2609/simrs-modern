package com.vone.simrs.laborat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class LaboratLineItemRequest {
    @NotBlank
    private String lineType;
    private Integer refId;
    @NotBlank
    private String code;
    private String description;
    @Min(0)
    private double quantity;
    @Min(0)
    private double unitPrice;
    private double discountAmount;
    private String discountType;

    public String getLineType() { return lineType; }
    public void setLineType(String lineType) { this.lineType = lineType; }
    public Integer getRefId() { return refId; }
    public void setRefId(Integer refId) { this.refId = refId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
}
