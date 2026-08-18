package com.vone.simrs.ward;

/**
 * Riwayat inventory pasien per item (tree HISTORY INVENTORY PASIEN). Migrasi
 * dari {@code PatientInventoryManagerImpl.getHistoryInventory()}.
 */
public class WardPatientInventoryHistoryResponse {

    private final Integer piId;
    private final Integer itemId;
    private final String date;
    private final Integer qtyIn;
    private final Integer qtyOut;
    private final Integer sisa;

    public WardPatientInventoryHistoryResponse(Integer piId, Integer itemId, String date,
            Integer qtyIn, Integer qtyOut, Integer sisa) {
        this.piId = piId;
        this.itemId = itemId;
        this.date = date;
        this.qtyIn = qtyIn;
        this.qtyOut = qtyOut;
        this.sisa = sisa;
    }

    public Integer getPiId() {
        return piId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getDate() {
        return date;
    }

    public Integer getQtyIn() {
        return qtyIn;
    }

    public Integer getQtyOut() {
        return qtyOut;
    }

    public Integer getSisa() {
        return sisa;
    }
}
