package com.vone.simrs.master.iteminventory;

import java.math.BigDecimal;

/**
 * Baris data laporan O-BM yang hampir kadaluwarsa (SC0190).
 * Mengikuti entity legacy {@code TbItemInventory} (tabel tb_item_inventory)
 * beserta relasi item (ms_item), batch (tb_batch_item), dan gudang
 * (ms_warehouse).
 */
public class ExpiredItemReportResponse {

    private final String itemCode;
    private final String itemName;
    private final String batchNo;
    private final String expDate;
    private final String whouseName;
    private final BigDecimal qty;

    public ExpiredItemReportResponse(String itemCode, String itemName, String batchNo,
            String expDate, String whouseName, BigDecimal qty) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.batchNo = batchNo;
        this.expDate = expDate;
        this.whouseName = whouseName;
        this.qty = qty;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public String getExpDate() {
        return expDate;
    }

    public String getWhouseName() {
        return whouseName;
    }

    public BigDecimal getQty() {
        return qty;
    }
}
