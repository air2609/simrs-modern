package com.vone.simrs.master.item;

import java.util.List;

/**
 * Request simpan/edit item (SCM0038). Mengikuti field yang diisi pada form
 * legacy {@code ItemController} (kode, nama, group, satuan, supplier, dll).
 */
public class ItemSaveRequest {

    private Integer id;
    private String itemCode;
    private String itemName;
    private String barcodeNo;
    private Integer itemGroupId;
    private Integer measurementId;
    private String itemReturnable;
    private Short itemType;
    private Short r;
    private Short bufferLimit;
    private Short plafon;
    private Integer maxOrder;
    private List<Integer> supplierIds;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getBarcodeNo() {
        return barcodeNo;
    }

    public void setBarcodeNo(String barcodeNo) {
        this.barcodeNo = barcodeNo;
    }

    public Integer getItemGroupId() {
        return itemGroupId;
    }

    public void setItemGroupId(Integer itemGroupId) {
        this.itemGroupId = itemGroupId;
    }

    public Integer getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId(Integer measurementId) {
        this.measurementId = measurementId;
    }

    public String getItemReturnable() {
        return itemReturnable;
    }

    public void setItemReturnable(String itemReturnable) {
        this.itemReturnable = itemReturnable;
    }

    public Short getItemType() {
        return itemType;
    }

    public void setItemType(Short itemType) {
        this.itemType = itemType;
    }

    public Short getR() {
        return r;
    }

    public void setR(Short r) {
        this.r = r;
    }

    public Short getBufferLimit() {
        return bufferLimit;
    }

    public void setBufferLimit(Short bufferLimit) {
        this.bufferLimit = bufferLimit;
    }

    public Short getPlafon() {
        return plafon;
    }

    public void setPlafon(Short plafon) {
        this.plafon = plafon;
    }

    public Integer getMaxOrder() {
        return maxOrder;
    }

    public void setMaxOrder(Integer maxOrder) {
        this.maxOrder = maxOrder;
    }

    public List<Integer> getSupplierIds() {
        return supplierIds;
    }

    public void setSupplierIds(List<Integer> supplierIds) {
        this.supplierIds = supplierIds;
    }
}
