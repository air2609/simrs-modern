package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Request ubah OP yang masih OPEN (SC0193). Migrasi dari legacy
 * {@code POController.doSaveModify()}.
 */
public class PurchaseOrderUpdateRequest {

    private String poCode;
    private String dueDate; // ISO yyyy-MM-dd
    private Double subtotal;
    private Double discount;
    private String discountType; // RP | %
    private Double total;
    private List<PurchaseOrderSaveRequest.Line> lines;

    public String getPoCode() {
        return poCode;
    }

    public void setPoCode(String poCode) {
        this.poCode = poCode;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<PurchaseOrderSaveRequest.Line> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseOrderSaveRequest.Line> lines) {
        this.lines = lines;
    }
}
