package com.vone.simrs.purchasing;

import java.util.List;

/**
 * Data master untuk screen SC0193 (ORDER PEMBELIAN): daftar unit LOKASI
 * TRANSAKSI untuk screen SC0193 dan daftar satuan (measurement). Migrasi dari
 * legacy {@code POController.init()} yang mengisi listbox {@code location}
 * dari {@code MsUnitByScreenCode(ORDER_PEMBELIAN)}.
 */
public class PurchaseOrderMastersResponse {

    private final List<PurchaseRequestUnitResponse> units;
    private final List<PurchaseRequestMeasurementResponse> measurements;

    public PurchaseOrderMastersResponse(List<PurchaseRequestUnitResponse> units,
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
