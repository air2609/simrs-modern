package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Header + detail OP yang dipilih pada bandbox NO. OP screen SC0195 untuk
 * dibuatkan BPP. Migrasi dari legacy {@code DOManagerImpl.redraw(DOController,
 * TbPurchaseOrder)} yang mengisi kolom KODE, KETERANGAN, ORD/A, BONUS, SATUAN,
 * HRG SAT, ORD/S, ORD/T, BNS/S, BNS/T, DISKON, dan SUBTOTAL.
 */
public class DeliveryOrderPoDetailResponse {

    private final String poCode;
    private final String status;
    private final Integer supplierId;
    private final String supplierCode;
    private final String supplierName;
    private final String supplierAddress;
    private final String supplierTelp;
    private final Double discount;
    private final String discountType;
    private final List<Line> items;

    public DeliveryOrderPoDetailResponse(String poCode, String status, Integer supplierId,
            String supplierCode, String supplierName, String supplierAddress, String supplierTelp,
            Double discount, String discountType, List<Line> items) {
        this.poCode = poCode;
        this.status = status;
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.supplierAddress = supplierAddress;
        this.supplierTelp = supplierTelp;
        this.discount = discount;
        this.discountType = discountType;
        this.items = items;
    }

    public String getPoCode() {
        return poCode;
    }

    public String getStatus() {
        return status;
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

    public Double getDiscount() {
        return discount;
    }

    public String getDiscountType() {
        return discountType;
    }

    public List<Line> getItems() {
        return items;
    }

    public static class Line {

        private final Integer poDetId;
        private final Integer itemId;
        private final String itemCode;
        private final String itemName;
        private final Integer qtyOrdered;
        private final Integer bonus;
        private final String measurementCode;
        private final Double cost;
        private final Integer qtySisa;
        private final Integer bonusSisa;
        private final Double discount;
        private final String discountType;

        public Line(Integer poDetId, Integer itemId, String itemCode, String itemName,
                Integer qtyOrdered, Integer bonus, String measurementCode, Double cost,
                Integer qtySisa, Integer bonusSisa, Double discount, String discountType) {
            this.poDetId = poDetId;
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.qtyOrdered = qtyOrdered;
            this.bonus = bonus;
            this.measurementCode = measurementCode;
            this.cost = cost;
            this.qtySisa = qtySisa;
            this.bonusSisa = bonusSisa;
            this.discount = discount;
            this.discountType = discountType;
        }

        public Integer getPoDetId() {
            return poDetId;
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

        public Integer getQtyOrdered() {
            return qtyOrdered;
        }

        public Integer getBonus() {
            return bonus;
        }

        public String getMeasurementCode() {
            return measurementCode;
        }

        public Double getCost() {
            return cost;
        }

        public Integer getQtySisa() {
            return qtySisa;
        }

        public Integer getBonusSisa() {
            return bonusSisa;
        }

        public Double getDiscount() {
            return discount;
        }

        public String getDiscountType() {
            return discountType;
        }
    }
}
