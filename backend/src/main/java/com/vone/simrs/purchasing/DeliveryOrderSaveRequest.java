package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Request simpan BPP baru (SC0195). Migrasi dari legacy
 * {@code DOController.doSaveAdd()} + {@code DOManagerImpl.doSaveAdd(...)}.
 */
public class DeliveryOrderSaveRequest {

    private Integer warehouseId;
    private String poCode;
    private String doCode;
    private String recDate; // ISO yyyy-MM-dd
    private Double ppn;
    private String ppnType; // RP | %
    private Double discount;
    private String discountType; // RP | %
    private Double total;
    private Double gtotal;
    private List<Line> lines;

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getPoCode() {
        return poCode;
    }

    public void setPoCode(String poCode) {
        this.poCode = poCode;
    }

    public String getDoCode() {
        return doCode;
    }

    public void setDoCode(String doCode) {
        this.doCode = doCode;
    }

    public String getRecDate() {
        return recDate;
    }

    public void setRecDate(String recDate) {
        this.recDate = recDate;
    }

    public Double getPpn() {
        return ppn;
    }

    public void setPpn(Double ppn) {
        this.ppn = ppn;
    }

    public String getPpnType() {
        return ppnType;
    }

    public void setPpnType(String ppnType) {
        this.ppnType = ppnType;
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

    public Double getGtotal() {
        return gtotal;
    }

    public void setGtotal(Double gtotal) {
        this.gtotal = gtotal;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public static class Line {

        private Integer poDetId;
        private Integer itemId;
        private Integer qtyArrived;
        private Integer bonusArrived;
        private Double subtotal;

        public Integer getPoDetId() {
            return poDetId;
        }

        public void setPoDetId(Integer poDetId) {
            this.poDetId = poDetId;
        }

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public Integer getQtyArrived() {
            return qtyArrived;
        }

        public void setQtyArrived(Integer qtyArrived) {
            this.qtyArrived = qtyArrived;
        }

        public Integer getBonusArrived() {
            return bonusArrived;
        }

        public void setBonusArrived(Integer bonusArrived) {
            this.bonusArrived = bonusArrived;
        }

        public Double getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(Double subtotal) {
            this.subtotal = subtotal;
        }
    }
}
