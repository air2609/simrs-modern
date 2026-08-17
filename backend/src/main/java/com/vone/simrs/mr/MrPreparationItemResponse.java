package com.vone.simrs.mr;

/**
 * DTO untuk satu baris berkas rekam medis pada screen SC0006 (PERSIAPAN DOKUMEN
 * REKAM MEDIS).
 */
public class MrPreparationItemResponse {

    private final Integer regId;
    private final String mrCode;
    private final String patientName;
    private final String unitName;

    public MrPreparationItemResponse(Integer regId, String mrCode, String patientName, String unitName) {
        this.regId = regId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.unitName = unitName;
    }

    public Integer getRegId() {
        return regId;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getUnitName() {
        return unitName;
    }
}
