package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Header + detail OP yang dimuat pada screen persetujuan (SC0194) saat
 * memilih/mengganti NO. OP. Migrasi dari legacy
 * {@code POApproval.redraw()} + {@code POManagerImpl.redraw(POApproval, ...)}.
 */
public class PurchaseOrderApprovalDetailResponse {

    private final String poCode;
    private final String status;
    private final String issuerName;
    private final String approvedByName;
    private final List<Item> items;

    public PurchaseOrderApprovalDetailResponse(String poCode, String status, String issuerName,
            String approvedByName, List<Item> items) {
        this.poCode = poCode;
        this.status = status;
        this.issuerName = issuerName;
        this.approvedByName = approvedByName;
        this.items = items;
    }

    public String getPoCode() {
        return poCode;
    }

    public String getStatus() {
        return status;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {

        private final Integer itemId;
        private final String itemCode;
        private final String itemName;
        private final Double cost;
        private final Integer qtyOrdered;
        private final String measurementCode;
        private final Integer bonus;
        private final Double discount;
        private final String discountType;
        private final Double subtotal;

        public Item(Integer itemId, String itemCode, String itemName, Double cost,
                Integer qtyOrdered, String measurementCode, Integer bonus, Double discount,
                String discountType, Double subtotal) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.cost = cost;
            this.qtyOrdered = qtyOrdered;
            this.measurementCode = measurementCode;
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

        public Double getCost() {
            return cost;
        }

        public Integer getQtyOrdered() {
            return qtyOrdered;
        }

        public String getMeasurementCode() {
            return measurementCode;
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
