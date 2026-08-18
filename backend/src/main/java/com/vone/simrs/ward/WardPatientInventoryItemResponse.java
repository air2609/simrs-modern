package com.vone.simrs.ward;

import java.util.List;

/**
 * Baris inventory pasien (modal INVENTORY PASIEN). Migrasi dari legacy
 * {@code PatientInventoryManagerImpl.getPatientInventory()} — total masuk,
 * terpakai, sisa per item.
 */
public class WardPatientInventoryItemResponse {

    private final Integer itemId;
    private final String code;
    private final String name;
    private final String unit;
    private final Integer totalIn;
    private final Integer totalOut;
    private final Integer sisa;

    public WardPatientInventoryItemResponse(Integer itemId, String code, String name, String unit,
            Integer totalIn, Integer totalOut, Integer sisa) {
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.totalIn = totalIn;
        this.totalOut = totalOut;
        this.sisa = sisa;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getTotalIn() {
        return totalIn;
    }

    public Integer getTotalOut() {
        return totalOut;
    }

    public Integer getSisa() {
        return sisa;
    }
}
