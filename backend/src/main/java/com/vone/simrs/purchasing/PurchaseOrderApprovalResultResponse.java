package com.vone.simrs.purchasing;

/**
 * Hasil aksi DISETUJUI pada screen persetujuan OP (SC0194). Migrasi dari
 * legacy {@code POManagerImpl.doApprove(POApproval, TbPurchaseOrder)} yang
 * mengubah status OP menjadi APPROVED dan mengisi kolom DISETUJUI OLEH.
 */
public class PurchaseOrderApprovalResultResponse {

    private final String poCode;
    private final String status;
    private final String approvedByName;

    public PurchaseOrderApprovalResultResponse(String poCode, String status, String approvedByName) {
        this.poCode = poCode;
        this.status = status;
        this.approvedByName = approvedByName;
    }

    public String getPoCode() {
        return poCode;
    }

    public String getStatus() {
        return status;
    }

    public String getApprovedByName() {
        return approvedByName;
    }
}
