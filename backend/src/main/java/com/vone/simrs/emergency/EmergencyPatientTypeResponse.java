package com.vone.simrs.emergency;

/**
 * Tipe pasien (ms_patient_type). Migrasi dari legacy
 * {@code PatientTypeController.getAllPatientTypeList2()}.
 */
public class EmergencyPatientTypeResponse {

    private final Integer patientTypeId;
    private final String code;
    private final String description;

    public EmergencyPatientTypeResponse(Integer patientTypeId, String code, String description) {
        this.patientTypeId = patientTypeId;
        this.code = code;
        this.description = description;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
