package com.vone.simrs.master.batchitem;

import java.util.List;

/**
 * Request simpan batch item (SCM0055 - UPDATE BATCH ITEM).
 * Mengikuti {@code ItemManager.updateItemBatch()}.
 */
public class BatchItemSaveRequest {

    private List<BatchItemRowResponse> items;

    public List<BatchItemRowResponse> getItems() {
        return items;
    }

    public void setItems(List<BatchItemRowResponse> items) {
        this.items = items;
    }
}
