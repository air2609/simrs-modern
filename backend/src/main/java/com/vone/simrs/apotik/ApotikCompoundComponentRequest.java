package com.vone.simrs.apotik;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

public class ApotikCompoundComponentRequest {

    @NotNull
    private Integer referenceId;

    @NotNull
    @DecimalMin(value = "0.0001")
    private Double quantity;

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
}
