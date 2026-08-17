package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Request simpan OPP baru (SC0191). Migrasi dari legacy
 * {@code PORManagerImpl.doSaveAddPORController()}.
 */
public class PurchaseRequestSaveRequest {

    private Integer unitId;
    private Integer supplierId;
    private List<PurchaseRequestLineRequest> lines;

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