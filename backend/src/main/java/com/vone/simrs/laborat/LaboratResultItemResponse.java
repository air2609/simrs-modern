package com.vone.simrs.laborat;

/**
 * Item hasil lab individual dalam tree view (SC0043).
 */
public class LaboratResultItemResponse {

    private final Integer detailId;
    private final Integer treatmentId;
    private final String treatmentName;
    private final String detailName;
    private final String groupName;
    private final String resultDescription;
    private final String normalRangeMan;
    private final String normalRangeWoman;
    private final String quantityUnit;
    private final Integer labDetilId;

    public LaboratResultItemResponse(Integer detailId, Integer treatmentId, String treatmentName,
            String detailName, String groupName, String resultDescription,
            String normalRangeMan, String normalRangeWoman, String quantityUnit, Integer labDetilId) {
        this.detailId = detailId;
        this.treatmentId = treatmentId;
        this.treatmentName = treatmentName;
        this.detailName = detailName;
        this.groupName = groupName;
        this.resultDescription = resultDescription;
        this.normalRangeMan = normalRangeMan;
        this.normalRangeWoman = normalRangeWoman;
        this.quantityUnit = quantityUnit;
        this.labDetilId = labDetilId;
    }

    public Integer getDetailId() { return detailId; }
    public Integer getTreatmentId() { return treatmentId; }
    public String getTreatmentName() { return treatmentName; }
    public String getDetailName() { return detailName; }
    public String getGroupName() { return groupName; }
    public String getResultDescription() { return resultDescription; }
    public String getNormalRangeMan() { return normalRangeMan; }
    public String getNormalRangeWoman() { return normalRangeWoman; }
    public String getQuantityUnit() { return quantityUnit; }
    public Integer getLabDetilId() { return labDetilId; }
}
