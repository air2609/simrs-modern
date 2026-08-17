package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Data tab INPUT BATCH NO. (SC0195B): daftar item BPP beserta JUMLAH AWAL dan
 * SATUAN AWAL. Migrasi dari legacy
 * {@code DOManagerImpl.redraw(DOBatchController)}.
 */
public class DeliveryOrderBatchMastersResponse {

    private final String doCode;
    private final List<BatchItem> items;

    public DeliveryOrderBatchMastersResponse(String doCode, List<BatchItem> items) {
        this.doCode = doCode;
        this.items = items;
    }

    public String getDoCode() {
        return doCode;
    }

    public List<BatchItem> getItems() {
        return items;
    }

    public static class BatchItem {

        private final Integer itemId;
        private final String itemCode;
        private final String itemName;
        private final Integer initQty;
        private final String initM;

        public BatchItem(Integer itemId, String itemCode, String itemName, Integer initQty,
                String initM) {
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.initQty = initQty;
            this.initM = initM;
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

        public Integer getInitQty() {
            return initQty;
        }

        public String getInitM() {
            return initM;
        }
    }
}
