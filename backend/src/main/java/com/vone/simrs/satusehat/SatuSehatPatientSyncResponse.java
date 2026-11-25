package com.vone.simrs.satusehat;

/**
 * Hasil sinkronisasi data pasien ke SATUSEHAT.
 */
public class SatuSehatPatientSyncResponse {

    private final String mrCode;
    private final String nik;
    private final String ihsNumber;
    /** CREATED, FOUND, atau UPDATED. */
    private final String action;
    private final String message;

    public SatuSehatPatientSyncResponse(String mrCode, String nik, String ihsNumber,
            String action, String message) {
        this.mrCode = mrCode;
        this.nik = nik;
        this.ihsNumber = ihsNumber;
        this.action = action;
        this.message = message;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getNik() {
        return nik;
    }

    public String getIhsNumber() {
        return ihsNumber;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }
}
