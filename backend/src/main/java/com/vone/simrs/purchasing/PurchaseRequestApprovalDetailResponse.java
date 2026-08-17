package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Header + detail OPP yang dimuat pada screen persetujuan (SC0192) saat
 * memilih/mengganti NO. OPP. Migrasi dari legacy
 * {@code PORApproval.redraw()} + {@code PORManagerImpl.redrawPORApproval()}.
 *
 * <p>
 * Kolom DISETUJUI OLEH diisi dari {@code ms_staff} yang menandatangani
 * persetujuan ({@code n_approver_id}); untuk OPP yang masih OPEN nilainya
 * kosong.
 */
public class PurchaseRequestApprovalDetailResponse {

    private final String prCode;
    private final String status;
    private final String issuerName;
    private final String approvedByName;
    private final Integer unitId;
    private final String unitName;
    private final Integer warehouseId;
    private final Integer supplierId;
    private final String supplierCode;
    private final String supplierName;
    private final String supplierAddress;
    private final String supplierTelp;
    private final List<PurchaseRequestApprovalItemResponse> items;

    public PurchaseRequestApprovalDetailResponse(String prCode, String status, String issuerName,
            String approvedByName, Integer unitId, String unitName, Integer warehouseId,
            Integer supplierId, String supplierCode, String supplierName, String supplierAddress,
            String supplierTelp, List<PurchaseRequestApprovalItemResponse> items) {
        this.prCode = prCode;
        this.status = status;
        this.issuerName = issuerName;
        this.approvedByName = approvedByName;
        this.unitId = unitId;
        this.unitName = unitName;
        this.warehouseId = warehouseId;
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.supplierAddress = supplierAddress;
        this.supplierTelp = supplierTelp;
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

    public String getApprovedByName() {
        return approvedByName;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getSupplierAddress() {
        return supplierAddress;
    }

    public String getSupplierTelp() {
        return supplierTelp;
    }

    public List<PurchaseRequestApprovalItemResponse> getItems() {
        return items;
    }
}
