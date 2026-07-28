package com.vone.simrs.master.treatment;

public class LabTreatmentDetailResponse {

    private final Integer detailId;
    private final Integer treatmentId;
    private final String detailName;
    private final String quantify;
    private final String normalRange;

    public LabTreatmentDetailResponse(Integer detailId, Integer treatmentId, String detailName, String quantify, String normalRange) {
        this.detailId = detailId;
        this.treatmentId = treatmentId;
        this.detailName = detailName;
        this.quantify = quantify;
        this.normalRange = normalRange;
    }

    public Integer getDetailId() {
        return detailId;
    }

    public Integer getTreatmentId() {
        return treatmentId;
    }

    public String getDetailName() {
        return detailName;
    }

    public String getQuantify() {
        return quantify;
    }

    public String getNormalRange() {
        return normalRange;
    }
}
