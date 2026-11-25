package com.vone.simrs.satusehat;

/**
 * Baris mapping analyte lab -> kode LOINC.
 */
public class SatuSehatLabMappingRow {

    private final Integer detailId;
    private final Integer treatmentId;
    private final String treatmentCode;
    private final String treatmentName;
    private final String analyteName;
    private final String unit;
    private final String loincCode;
    private final String loincDisplay;
    private final String ucumCode;

    public SatuSehatLabMappingRow(Integer detailId, Integer treatmentId, String treatmentCode,
            String treatmentName, String analyteName, String unit, String loincCode,
            String loincDisplay, String ucumCode) {
        this.detailId = detailId;
        this.treatmentId = treatmentId;
        this.treatmentCode = treatmentCode;
        this.treatmentName = treatmentName;
        this.analyteName = analyteName;
        this.unit = unit;
        this.loincCode = loincCode;
        this.loincDisplay = loincDisplay;
        this.ucumCode = ucumCode;
    }

    public Integer getDetailId() {
        return detailId;
    }

    public Integer getTreatmentId() {
        return treatmentId;
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public String getAnalyteName() {
        return analyteName;
    }

    public String getUnit() {
        return unit;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public String getLoincDisplay() {
        return loincDisplay;
    }

    public String getUcumCode() {
        return ucumCode;
    }
}
