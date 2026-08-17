package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Header + detail OP (ORDER PEMBELIAN) yang dimuat dari bandbox NO. OP
 * (SC0193) untuk diubah. Migrasi dari legacy
 * {@code POManagerImpl.redrawPO()}.
 */
public class PurchaseOrderPoDetailResponse {

    private final String poCode;
    private final String status;
    private final String oppNo;
    private final String issuerName;
    private final String approvedByName;
    private final Integer supplierId;
    private final String supplierCode;
    private final String supplierName;
    private final String supplierAddress;
    private final String supplierTelp;
    private final String dueDate;
    private final Double subtotal;
    private final Double discount;
    private final String discountType;
    private final Double total;
    private final List<Line> items;

    public PurchaseOrderPoDetailResponse(String poCode, String status, String oppNo,
            String issuerName, String approvedByName, Integer supplierId, String supplierCode,
            String supplierName, String supplierAddress, String supplierTelp, String dueDate,
            Double subtotal, Double discount, String discountType, Double total, List<Line> items) {
        this.poCode = poCode;
        this.status = status;
        this.oppNo = oppNo;
        this.issuerName = issuerName;
        this.approvedByName = approvedByName;
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.supplierAddress = supplierAddress;
        this.supplierTelp = supplierTelp;
        this.dueDate = dueDate;
        this.subtotal = subtotal;
        this.discount = discount;
        this.discountType = discountType;
        this.total = total;
        this.items = items;
    }

    public String getPoCode() {
        return poCode;
    }

    public String getStatus() {
        return status;
    }

    public String getOppNo() {
        return oppNo;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getSupplierAddress() {
        return supplierAddress;
    }

    public String getSupplierTelp() {
        return supplierTelp;
    }

    public String getDueDate() {
        return dueDate;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public Double getTotal() {
        return total;
    }

    public List<Line> getItems() {
        return items;
    }

    public static class Line {

        private final Integer itemId;
        private final String itemCode;
        private final String itemName;
        private final String measurementCode;
        private final Integer measurementId;
        private final Double cost;
        private final Integer qtyOrdered;
        private final Integer bonus;
        private final Double discount;
        private final String discountType;
        private final Double subtotal;

        public Line(Integer itemId, String itemCode, String itemName, String measurementCode,
                Integer measurementId, Double cost, Integer qtyOrdered, Integer bonus,
                Double discount, String discountType, Double subtotal) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.measurementCode = measurementCode;
            this.measurementId = measurementId;
            this.cost = cost;
            this.qtyOrdered = qtyOrdered;
            this.bonus = bonus;
            this.discount = discount;
            this.discountType = discountType;
            this.subtotal = subtotal;
        }

        public Integer getItemId() {
            return itemId;
        }

        public String getItemCode() {
            return itemCode;
        }

        public String getItemName() {
            return itemName;
        }

        public String getMeasurementCode() {
            return measurementCode;
        }

        public Integer getMeasurementId() {
            return measurementId;
        }

        public Double getCost() {
            return cost;
        }

        public Integer getQtyOrdered() {
            return qtyOrdered;
        }

        public Integer getBonus() {
            return bonus;
        }

        public Double getDiscount() {
            return discount;
        }

        public String getDiscountType() {
            return discountType;
        }

        public Double getSubtotal() {
            return subtotal;
        }
    }
}
