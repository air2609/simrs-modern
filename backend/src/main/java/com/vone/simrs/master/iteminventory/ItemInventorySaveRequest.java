package com.vone.simrs.master.iteminventory;

import java.math.BigDecimal;

/**
 * Request simpan/edit alokasi item (SCM0032). Mengikuti field yang diisi
 * pada form legacy {@code UnitInventoryController} (kode item, batch no,
 * jumlah, harga beli) dan lokasi gudang.
 */
public class ItemInventorySaveRequest {

    private Integer id;
    private Integer whouseId;
    private Integer itemId;
    private String itemCode;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal cogsPrice;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getWhouseId() {
        return whouseId;
    }

    public void setWhouseId(Integer whouseId) {
        this.whouseId = whouseId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getCogsPrice() {
        return cogsPrice;
    }

    public void setCogsPrice(BigDecimal cogsPrice) {
        this.cogsPrice = cogsPrice;
    }
}
