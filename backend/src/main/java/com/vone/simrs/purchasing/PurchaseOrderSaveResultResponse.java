package com.vone.simrs.purchasing;

/**
 * Hasil simpan OP baru (SC0193). Migrasi dari legacy
 * {@code POController.doSaveAdd()} yang menampilkan pesan sukses beserta NO.
 * OP yang dibuat dan mengisi field DIBUAT OLEH.
 */
public class PurchaseOrderSaveResultResponse {

    private final String poCode;
    private final String status;
    private final String issuerName;

    public PurchaseOrderSaveResultResponse(String poCode, String status, String issuerName) {
        this.poCode = poCode;
        this.status = status;
        this.issuerName = issuerName;
    }

    public String getPoCode() {
        return poCode;
    }

    public String getStatus() {
        return status;
    }

    public String getIssuerName() {
        return issuerName;
    }
}
