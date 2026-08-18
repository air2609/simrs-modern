package com.vone.simrs.warehouse;

import java.util.List;

/**
 * Grup item mutation yang menunggu persetujuan (tab PERSETUJUAN).
 * Migrasi dari legacy {@code WarehouseManagerImpl.getSentItem()}.
 */
public class ItemApprovalGroupResponse {

    private final String requestCode;
    private final String sourceWarehouseName;
    private final String targetWarehouseName;
    private final List<ItemApprovalRowResponse> items;

    public ItemApprovalGroupResponse(String requestCode, String sourceWarehouseName,
            String targetWarehouseName, List<ItemApprovalRowResponse> items) {
        this.requestCode = requestCode;
        this.sourceWarehouseName = sourceWarehouseName;
        this.targetWarehouseName = targetWarehouseName;
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

    public List<ItemApprovalRowResponse> getItems() {
        return items;
    }
}
