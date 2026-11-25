package com.vone.simrs.satusehat;

/**
 * Hasil sinkronisasi Encounter (kunjungan) ke SATUSEHAT.
 */
public class SatuSehatEncounterSyncResponse {

    private final String registrationCode;
    private final String ihsNumber;
    private final String encounterId;
    private final String status;
    /** CREATED, FINISHED, atau EXISTING. */
    private final String action;
    private final String message;

    public SatuSehatEncounterSyncResponse(String registrationCode, String ihsNumber, String encounterId,
            String status, String action, String message) {
        this.registrationCode = registrationCode;
        this.ihsNumber = ihsNumber;
        this.encounterId = encounterId;
        this.status = status;
        this.action = action;
        this.message = message;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public String getIhsNumber() {
        return ihsNumber;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getStatus() {
        return status;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }
}
