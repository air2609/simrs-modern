package com.vone.simrs.satusehat;

/**
 * Hasil sinkronisasi Condition (diagnosa) atau Procedure (tindakan) ke SATUSEHAT.
 */
public class SatuSehatClinicalSyncResponse {

    private final String registrationCode;
    private final String encounterId;
    /** "Condition" atau "Procedure". */
    private final String resourceType;
    private final int created;
    private final int skipped;
    private final String message;

    public SatuSehatClinicalSyncResponse(String registrationCode, String encounterId, String resourceType,
            int created, int skipped, String message) {
        this.registrationCode = registrationCode;
        this.encounterId = encounterId;
        this.resourceType = resourceType;
        this.created = created;
        this.skipped = skipped;
        this.message = message;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public int getCreated() {
        return created;
    }

    public int getSkipped() {
        return skipped;
    }

    public String getMessage() {
        return message;
    }
}
