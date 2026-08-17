package com.vone.simrs.purchasing;

/**
 * Hasil aksi DISETUJUI pada screen persetujuan (SC0192). Migrasi dari legacy
 * {@code PORManagerImpl.doApprove()} yang mengubah status OPP menjadi APPROVED
 * dan mengisi kolom DISETUJUI OLEH dengan staff yang menandatangani.
 */
public class PurchaseRequestApprovalResultResponse {

    private final String prCode;
    private final String status;
    private final String approvedByName;

    public PurchaseRequestApprovalResultResponse(String prCode, String status, String approvedByName) {
        this.prCode = prCode;
        this.status = status;
        this.approvedByName = approvedByName;
    }

    public String getPrCode() {
        return prCode;
    }

    public String getStatus() {
        return status;
    }

    public String getApprovedByName() {
        return approvedByName;
    }
}
