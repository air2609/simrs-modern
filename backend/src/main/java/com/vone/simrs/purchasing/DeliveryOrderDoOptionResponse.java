package com.vone.simrs.purchasing;

/**
 * Hasil pencarian BPP pada bandbox NO. BPP (SC0195). Migrasi dari legacy
 * {@code DOManagerImpl.doSearchDO()} yang menampilkan NO. DO, NAMA GUDANG,
 * dan tanggal untuk BPP berstatus OPEN.
 */
public class DeliveryOrderDoOptionResponse {

    private final String doCode;
    private final String warehouseName;
    private final String createdDate;

    public DeliveryOrderDoOptionResponse(String doCode, String warehouseName, String createdDate) {
        this.doCode = doCode;
        this.warehouseName = warehouseName;
        this.createdDate = createdDate;
    }

    public String getDoCode() {
        return doCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getCreatedDate() {
        return createdDate;
    }
}
