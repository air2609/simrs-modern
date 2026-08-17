package com.vone.simrs.purchasing;

/**
 * Hasil pencarian OP (ORDER PEMBELIAN) pada bandbox NO. OP (SC0193). Migrasi
 * dari legacy {@code POManagerImpl.doSearchPO()} yang menampilkan NO. PO,
 * NAMA SUPPLIER, dan tanggal pembuatan untuk OP yang statusnya bukan CLOSED.
 */
public class PurchaseOrderPoOptionResponse {

    private final String poCode;
    private final String supplierName;
    private final String createdDate;

    public PurchaseOrderPoOptionResponse(String poCode, String supplierName, String createdDate) {
        this.poCode = poCode;
        this.supplierName = supplierName;
        this.createdDate = createdDate;
    }

    public String getPoCode() {
        return poCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getCreatedDate() {
        return createdDate;
    }
}
