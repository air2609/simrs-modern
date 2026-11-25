package com.vone.simrs.satusehat;

/**
 * Baris mapping item obat -> kode KFA.
 */
public class SatuSehatItemMappingRow {

    private final Integer localId;
    private final String itemCode;
    private final String itemName;
    private final String kfaCode;
    private final String kfaDisplay;
    private final String ucumCode;
    private final boolean synced;

    public SatuSehatItemMappingRow(Integer localId, String itemCode, String itemName, String kfaCode,
            String kfaDisplay, String ucumCode, boolean synced) {
        this.localId = localId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.kfaCode = kfaCode;
        this.kfaDisplay = kfaDisplay;
        this.ucumCode = ucumCode;
        this.synced = synced;
    }

    public Integer getLocalId() {
        return localId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getKfaCode() {
        return kfaCode;
    }

    public String getKfaDisplay() {
        return kfaDisplay;
    }

    public String getUcumCode() {
        return ucumCode;
    }

    public boolean isSynced() {
        return synced;
    }
}
