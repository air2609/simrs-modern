package com.vone.simrs.master.iteminventory;

import java.math.BigDecimal;

/**
 * Baris data alokasi item (SCM0032). Mengikuti entity legacy
 * {@code TbItemInventory} (tabel tb_item_inventory) beserta relasi
 * item (ms_item) dan batch (tb_batch_item).
 */
public class ItemInventoryRowResponse {

    private final Integer id;
    private final Integer itemId;
    private final String itemCode;
    private final String itemName;
    private final Integer batchId;
    private final String batchNo;
    private final Integer whouseId;
    private final String whouseName;
    private final BigDecimal qty;
    private final BigDecimal cogsPrice;

    public ItemInventoryRowResponse(Integer id, Integer itemId, String itemCode, String itemName,
            Integer batchId, String batchNo, Integer whouseId, String whouseName,
            BigDecimal qty, BigDecimal cogsPrice) {
        this.id = id;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.batchId = batchId;
        this.batchNo = batchNo;
        this.whouseId = whouseId;
        this.whouseName = whouseName;
        this.qty = qty;
        this.cogsPrice = cogsPrice;
    }

    public Integer getId() {
        return id;
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

    public Integer getBatchId() {
        return batchId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public Integer getWhouseId() {
        return whouseId;
    }

    public String getWhouseName() {
        return whouseName;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public BigDecimal getCogsPrice() {
        return cogsPrice;
    }
}
