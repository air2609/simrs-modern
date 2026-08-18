package com.vone.simrs.warehouse;

import java.util.List;

/**
 * Request kirim mutasi item. Migrasi dari legacy
 * {@code WarehouseController.kirimClick()}.
 */
public class ItemMutationSendRequest {

    private Integer warehouseId;
    private List<ItemMutationLineRequest> lines;

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<ItemMutationLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<ItemMutationLineRequest> lines) {
        this.lines = lines;
    }
}
