package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Request ubah BPP yang masih OPEN (SC0195). Migrasi dari legacy
 * {@code DOController.doSaveModify()} + {@code DOManagerImpl.update(...)}.
 */
public class DeliveryOrderUpdateRequest {

    private String doCode;
    private Integer warehouseId;
    private String recDate; // ISO yyyy-MM-dd
    private Double ppn;
    private String ppnType; // RP | %
    private Double discount;
    private String discountType; // RP | %
    private Double total;
    private Double gtotal;
    private List<DeliveryOrderSaveRequest.Line> lines;

    public String getDoCode() {
        return doCode;
    }

    public void setDoCode(String doCode) {
        this.doCode = doCode;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
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

    public List<DeliveryOrderSaveRequest.Line> getLines() {
        return lines;
    }

    public void setLines(List<DeliveryOrderSaveRequest.Line> lines) {
        this.lines = lines;
    }
}
