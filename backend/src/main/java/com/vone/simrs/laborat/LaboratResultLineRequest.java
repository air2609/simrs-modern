package com.vone.simrs.laborat;

/**
 * Request line item untuk menyimpan hasil lab detail (SC0043).
 */
public class LaboratResultLineRequest {

    private Integer detailId;           // tb_laboratory_result_detail id (null for new)
    private Integer treatmentId;        // n_treatment_id
    private Integer labDetilId;         // n_lab_detil_id dari ms_lab_treatment_detil
    private String resultDescription;   // v_lab_rslt_desc (hasil)
    private String normalRangeMan;      // v_nrml_range_man
    private String normalRangeWoman;    // v_nrml_range_woman
    private String quantityUnit;        // v_lab_rslt_quantify

    public Integer getDetailId() { return detailId; }
    public void setDetailId(Integer detailId) { this.detailId = detailId; }
    public Integer getTreatmentId() { return treatmentId; }
    public void setTreatmentId(Integer treatmentId) { this.treatmentId = treatmentId; }
    public Integer getLabDetilId() { return labDetilId; }
    public void setLabDetilId(Integer labDetilId) { this.labDetilId = labDetilId; }
    public String getResultDescription() { return resultDescription; }
    public void setResultDescription(String resultDescription) { this.resultDescription = resultDescription; }
    public String getNormalRangeMan() { return normalRangeMan; }
    public void setNormalRangeMan(String normalRangeMan) { this.normalRangeMan = normalRangeMan; }
    public String getNormalRangeWoman() { return normalRangeWoman; }
    public void setNormalRangeWoman(String normalRangeWoman) { this.normalRangeWoman = normalRangeWoman; }
    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }
}
