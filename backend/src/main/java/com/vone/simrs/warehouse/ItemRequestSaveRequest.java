package com.vone.simrs.warehouse;

import java.util.List;

/**
 * Request kirim permintaan O-BM. Migrasi dari legacy
 * {@code ItemRequestController.kirimClick()}.
 */
public class ItemRequestSaveRequest {

    private Integer sourceWarehouseId;
    private Integer targetWarehouseId;
    private List<ItemRequestLineRequest> lines;

    public Integer getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(Integer sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public Integer getTargetWarehouseId() {
        return targetWarehouseId;
    }

    public void setTargetWarehouseId(Integer targetWarehouseId) {
        this.targetWarehouseId = targetWarehouseId;
    }

    public List<ItemRequestLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<ItemRequestLineRequest> lines) {
        this.lines = lines;
    }
}
