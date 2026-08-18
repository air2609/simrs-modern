package com.vone.simrs.warehouse;

import java.util.List;

/**
 * Grup permintaan O-BM per nomor permintaan. Migrasi dari legacy
 * {@code ItemRequestApproveController.createGroupTree()} /
 * {@code HistoryRequestController.createGroupTree()}.
 */
public class ItemRequestGroupResponse {

    private final String requestCode;
    private final String sourceWarehouseName;
    private final String targetWarehouseName;
    private final Integer status;
    private final String statusLabel;
    private final String date;
    private final List<ItemRequestRowResponse> items;

    public ItemRequestGroupResponse(String requestCode, String sourceWarehouseName,
            String targetWarehouseName, Integer status, String statusLabel, String date,
            List<ItemRequestRowResponse> items) {
        this.requestCode = requestCode;
        this.sourceWarehouseName = sourceWarehouseName;
        this.targetWarehouseName = targetWarehouseName;
        this.status = status;
        this.statusLabel = statusLabel;
        this.date = date;
        this.items = items;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public String getSourceWarehouseName() {
        return sourceWarehouseName;
    }

    public String getTargetWarehouseName() {
        return targetWarehouseName;
    }

    public Integer getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getDate() {
        return date;
    }

    public List<ItemRequestRowResponse> getItems() {
        return items;
    }
}
