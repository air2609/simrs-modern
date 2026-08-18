package com.vone.simrs.emergency;

/**
 * Tipe pembawa pasien (ms_patient_escort). Migrasi dari legacy
 * {@code PatientEscortController.getPatientEscortForSelect()}.
 */
public class EmergencyEscortResponse {

    private final Integer escortId;
    private final String code;
    private final String type;

    public EmergencyEscortResponse(Integer escortId, String code, String type) {
        this.escortId = escortId;
        this.code = code;
        this.type = type;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
