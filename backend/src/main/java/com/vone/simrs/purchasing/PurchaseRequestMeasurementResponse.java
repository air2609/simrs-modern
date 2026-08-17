package com.vone.simrs.purchasing;

/**
 * Opsi satuan (ITEM MEASUREMENT) untuk dropdown SATUAN pada DAFTAR ORDER
 * PERMINTAAN PEMBELIAN (SC0191). Migrasi dari legacy
 * {@code ItemMeasurementManagerImpl.getMeasurementType()}.
 */
public class PurchaseRequestMeasurementResponse {

    private final Integer measurementId;
    private final String earlyQuantify;
    private final String endQuantify;

    public PurchaseRequestMeasurementResponse(Integer measurementId, String earlyQuantify,
            String endQuantify) {
        this.measurementId = measurementId;
        this.earlyQuantify = earlyQuantify;
        this.endQuantify = endQuantify;
    }

    public Integer getMeasurementId() {
        return measurementId;
    }

    public String getEarlyQuantify() {
        return earlyQuantify;
    }

    public String getEndQuantify() {
        return endQuantify;
    }
}