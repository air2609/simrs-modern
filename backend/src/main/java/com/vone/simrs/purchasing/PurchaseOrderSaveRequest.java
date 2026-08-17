package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Request simpan OP baru (SC0193). Migrasi dari legacy
 * {@code POController.doSaveAdd()}.
 */
public class PurchaseOrderSaveRequest {

    private String unitCode;
    private String prCode;
    private Integer supplierId;
    private String dueDate; // ISO yyyy-MM-dd
    private Double subtotal;
    private Double discount;
    private String discountType; // RP | %
    private Double total;
    private List<Line> lines;

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getPrCode() {
        return prCode;
    }

    public void setPrCode(String prCode) {
        this.prCode = prCode;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
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

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public static class Line {

        private Integer itemId;
        private Double cost;
        private Integer qtyOrdered;
        private Integer measurementId;
        private Integer bonus;
        private Double discount;
        private String discountType;

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public Double getCost() {
            return cost;
        }

        public void setCost(Double cost) {
            this.cost = cost;
        }

        public Integer getQtyOrdered() {
            return qtyOrdered;
        }

        public void setQtyOrdered(Integer qtyOrdered) {
            this.qtyOrdered = qtyOrdered;
        }

        public Integer getMeasurementId() {
            return measurementId;
        }

        public void setMeasurementId(Integer measurementId) {
            this.measurementId = measurementId;
        }

        public Integer getBonus() {
            return bonus;
        }

        public void setBonus(Integer bonus) {
            this.bonus = bonus;
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
    }
}
