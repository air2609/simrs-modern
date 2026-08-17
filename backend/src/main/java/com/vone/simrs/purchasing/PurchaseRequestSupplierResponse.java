package com.vone.simrs.purchasing;

/**
 * Hasil pencarian supplier/vendor pada bandbox SUPPLIER (SC0191).
 * Migrasi dari legacy {@code VendorManager.searchVendor()}.
 */
public class PurchaseRequestSupplierResponse {

    private final Integer vendorId;
    private final String vendorCode;
    private final String vendorName;
    private final String vendorAddress;
    private final String vendorContactNo;

    public PurchaseRequestSupplierResponse(Integer vendorId, String vendorCode, String vendorName,
            String vendorAddress, String vendorContactNo) {
        this.vendorId = vendorId;
        this.vendorCode = vendorCode;
        this.vendorName = vendorName;
        this.vendorAddress = vendorAddress;
        this.vendorContactNo = vendorContactNo;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getVendorAddress() {
        return vendorAddress;
    }

    public String getVendorContactNo() {
        return vendorContactNo;
    }
}