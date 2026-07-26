package com.vone.simrs.apotik;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public class ApotikLineItemRequest {

    @NotBlank
    private String lineType;

    private Integer referenceId;

    @NotNull
    @DecimalMin(value = "0.0001")
    private Double quantity;

    private Double unitPrice;
    private String discountType;
    private Double discountValue;
    private String description;
    private String unitName;
    private String instruction;
    private List<ApotikCompoundComponentRequest> components;

    public String getLineType() { return lineType; }
    public void setLineType(String lineType) { this.lineType = lineType; }
    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public List<ApotikCompoundComponentRequest> getComponents() { return components; }
    public void setComponents(List<ApotikCompoundComponentRequest> components) { this.components = components; }
}
