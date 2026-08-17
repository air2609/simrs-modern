package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Header + detail OPP (ORDER PERMINTAAN PEMBELIAN) yang dimuat dari bandbox
 * NO. OPP (SC0191). Migrasi dari legacy
 * {@code PORManagerImpl.redrawSearchPORController()}.
 */
public class PurchaseRequestOppDetailResponse {

    private final String prCode;
    private final String status;
    private final String issuerName;
    private final Integer unitId;
    private final String unitName;
    private final Integer supplierId;
    private final String supplierName;
    private final List<PurchaseRequestDetailResponse> items;

    public PurchaseRequestOppDetailResponse(String prCode, String status, String issuerName,
            Integer unitId, String unitName, Integer supplierId, String supplierName,
            List<PurchaseRequestDetailResponse> items) {
        this.prCode = prCode;
        this.status = status;
        this.issuerName = issuerName;
        this.unitId = unitId;
        this.unitName = unitName;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.items = items;
    }

    public String getPrCode() {
        return prCode;
    }

    public String getStatus() {
        return status;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public List<PurchaseRequestDetailResponse> getItems() {
        return items;
    }
}