package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Data master untuk form OPP (SC0191): opsi unit untuk LOKASI TRANSAKSI dan
 * opsi satuan untuk kolom SATUAN pada DAFTAR ORDER.
 */
public class PurchaseRequestMastersResponse {

    private final List<PurchaseRequestUnitResponse> units;
    private final List<PurchaseRequestMeasurementResponse> measurements;

    public PurchaseRequestMastersResponse(List<PurchaseRequestUnitResponse> units,
            List<PurchaseRequestMeasurementResponse> measurements) {
        this.units = units;
        this.measurements = measurements;
    }

    public List<PurchaseRequestUnitResponse> getUnits() {
        return units;
    }

    public List<PurchaseRequestMeasurementResponse> getMeasurements() {
        return measurements;
    }
}