package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Request ubah OPP (SC0191). Migrasi dari legacy
 * {@code PORManagerImpl.doSaveModifyPORController()}.
 */
public class PurchaseRequestUpdateRequest {

    private String prCode;
    private Integer unitId;
    private Integer supplierId;
    private List<PurchaseRequestLineRequest> lines;

    public String getPrCode() {
        return prCode;
    }

    public void setPrCode(String prCode) {
        this.prCode = prCode;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public List<PurchaseRequestLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseRequestLineRequest> lines) {
        this.lines = lines;
    }
}