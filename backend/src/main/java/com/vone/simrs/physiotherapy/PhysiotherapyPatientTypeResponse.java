package com.vone.simrs.physiotherapy;

/**
 * Tipe pasien (ms_patient_type) untuk select TIPE PASIEN screen SC0141.
 */
public class PhysiotherapyPatientTypeResponse {

    private final Integer patientTypeId;
    private final String code;
    private final String description;

    public PhysiotherapyPatientTypeResponse(Integer patientTypeId, String code,
            String description) {
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
