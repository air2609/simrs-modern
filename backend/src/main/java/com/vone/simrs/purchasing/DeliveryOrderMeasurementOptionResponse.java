package com.vone.simrs.purchasing;

/**
 * Opsi SATUAN AKHIR pada tab INPUT BATCH NO. (SC0195B). Migrasi dari legacy
 * {@code DOBatchController.redrawStatus()} yang memakai
 * {@code getMsItemMeasurementListByCode()} (konversi satuan + multiplier).
 */
public class DeliveryOrderMeasurementOptionResponse {

    private final String endQuantify;
    private final Integer multiplier;

    public DeliveryOrderMeasurementOptionResponse(String endQuantify, Integer multiplier) {
        this.endQuantify = endQuantify;
        this.multiplier = multiplier;
    }

    public String getEndQuantify() {
        return endQuantify;
    }

    public Integer getMultiplier() {
        return multiplier;
    }
}
