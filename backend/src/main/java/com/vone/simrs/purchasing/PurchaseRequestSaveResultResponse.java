package com.vone.simrs.purchasing;

/**
 * Hasil simpan OPP baru (SC0191). Migrasi dari legacy
 * {@code PORManagerImpl.doSaveAddPORController()} yang menghasilkan nomor OPP.
 */
public class PurchaseRequestSaveResultResponse {

    private final String prCode;

    public PurchaseRequestSaveResultResponse(String prCode) {
        this.prCode = prCode;
    }

    public String getPrCode() {
        return prCode;
    }
}