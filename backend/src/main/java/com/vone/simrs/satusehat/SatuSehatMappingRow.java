package com.vone.simrs.satusehat;

/**
 * Baris mapping data master lokal ke ID SATUSEHAT
 * (unit -> Location, dokter -> Practitioner).
 */
public class SatuSehatMappingRow {

    private final Integer localId;
    private final String localCode;
    private final String localName;
    private final String satuSehatId;

    public SatuSehatMappingRow(Integer localId, String localCode, String localName, String satuSehatId) {
        this.localId = localId;
        this.localCode = localCode;
        this.localName = localName;
        this.satuSehatId = satuSehatId;
    }

    public Integer getLocalId() {
        return localId;
    }

    public String getLocalCode() {
        return localCode;
    }

    public String getLocalName() {
        return localName;
    }

    public String getSatuSehatId() {
        return satuSehatId;
    }
}
