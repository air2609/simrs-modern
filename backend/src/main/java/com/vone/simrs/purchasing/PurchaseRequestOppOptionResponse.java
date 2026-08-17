package com.vone.simrs.purchasing;

/**
 * Hasil pencarian OPP (ORDER PERMINTAAN PEMBELIAN) pada bandbox NO. OPP
 * (SC0191). Migrasi dari legacy
 * {@code PORManagerImpl.doSearchPORController()} yang hanya menampilkan OPP
 * dengan status OPEN.
 */
public class PurchaseRequestOppOptionResponse {

    private final String prCode;
    private final String unitName;

    public PurchaseRequestOppOptionResponse(String prCode, String unitName) {
        this.prCode = prCode;
        this.unitName = unitName;
    }

    public String getPrCode() {
        return prCode;
    }

    public String getUnitName() {
        return unitName;
    }
}