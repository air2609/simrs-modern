package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Header + detail BPP yang dimuat dari bandbox NO. BPP (SC0195). Migrasi dari
 * legacy {@code DOManagerImpl.redrawExistingDO(DOController)}.
 */
public class DeliveryOrderDoDetailResponse {

    private final String doCode;
    private final String status;
    private final String poCode;
    private final Integer warehouseId;
    private final String warehouseCode;
    private final String warehouseName;
    private final String recDate;
    private final String recBy;
    private final String approvedByName;
    private final String supplierCode;
    private final String supplierName;
    private final String supplierAddress;
    private final String supplierTelp;
    private final Double discount;
    private final String discountType;
    private final Double ppn;
    private final String ppnType;
    private final Double total;
    private final Double discountAmt;
    private final Double gtotal;
    private final List<Line> items;

    public DeliveryOrderDoDetailResponse(String doCode, String status, String poCode,
            Integer warehouseId, String warehouseCode, String warehouseName, String recDate,
            String recBy, String approvedByName, String supplierCode, String supplierName,
            String supplierAddress, String supplierTelp, Double discount, String discountType,
            Double ppn, String ppnType, Double total, Double discountAmt, Double gtotal,
            List<Line> items) {
        this.doCode = doCode;
        this.status = status;
        this.poCode = poCode;
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.recDate = recDate;
        this.recBy = recBy;
        this.approvedByName = approvedByName;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.supplierAddress = supplierAddress;
        this.supplierTelp = supplierTelp;
        this.discount = discount;
        this.discountType = discountType;
        this.ppn = ppn;
        this.ppnType = ppnType;
        this.total = total;
        this.discountAmt = discountAmt;
        this.gtotal = gtotal;
        this.items = items;
    }

    public String getDoCode() {
        return doCode;
    }

    public String getStatus() {
        return status;
    }

    public String getPoCode() {
        return poCode;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getRecDate() {
        return recDate;
    }

    public String getRecBy() {
        return recBy;
    }

    public String getApprovedByName() {
        return approvedByName;
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

    public Double getPpn() {
        return ppn;
    }

    public String getPpnType() {
        return ppnType;
    }

    public Double getTotal() {
        return total;
    }

    public Double getDiscountAmt() {
        return discountAmt;
    }

    public Double getGtotal() {
        return gtotal;
    }

    public List<Line> getItems() {
        return items;
    }

    public static class Line {

        private final Integer doDetId;
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
        private final Integer qtyArrived;
        private final Integer bonusArrived;
        private final Double subtotal;

        public Line(Integer doDetId, Integer poDetId, Integer itemId, String itemCode,
                String itemName, Integer qtyOrdered, Integer bonus, String measurementCode,
                Double cost, Integer qtySisa, Integer bonusSisa, Integer qtyArrived,
                Integer bonusArrived, Double subtotal) {
            this.doDetId = doDetId;
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
            this.qtyArrived = qtyArrived;
            this.bonusArrived = bonusArrived;
            this.subtotal = subtotal;
        }

        public Integer getDoDetId() {
            return doDetId;
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

        public Integer getQtyArrived() {
            return qtyArrived;
        }

        public Integer getBonusArrived() {
            return bonusArrived;
        }

        public Double getSubtotal() {
            return subtotal;
        }
    }
}
