package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Request validasi/approval BPP (SC0195) beserta daftar entry batch yang
 * sudah dimasukkan pada tab INPUT BATCH NO. Migrasi dari legacy
 * {@code DOController.doApprove()} + {@code TbDeliveryOrderDAO.executeApproval()}.
 */
public class DeliveryOrderApproveRequest {

    private String doCode;
    private List<BatchEntry> entries;

    public String getDoCode() {
        return doCode;
    }

    public void setDoCode(String doCode) {
        this.doCode = doCode;
    }

    public List<BatchEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<BatchEntry> entries) {
        this.entries = entries;
    }

    public static class BatchEntry {

        private Integer itemId;
        private String batchNo;
        private Integer qty; // qty yang diinput user (belum × multiplier)
        private String expDate; // ISO yyyy-MM-dd
        private String finalM;
        private Integer multiplier;

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }

        public String getExpDate() {
            return expDate;
        }

        public void setExpDate(String expDate) {
            this.expDate = expDate;
        }

        public String getFinalM() {
            return finalM;
        }

        public void setFinalM(String finalM) {
            this.finalM = finalM;
        }

        public Integer getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Integer multiplier) {
            this.multiplier = multiplier;
        }
    }
}
