package com.vone.simrs.mr;

import com.vone.simrs.apotik.ApotikLineItemRequest;
import java.util.List;

/**
 * Request body untuk menyimpan diagnosa (dan opsional resep) pada screen
 * SC0206.
 */
public class DiagnoseSaveRequestBody {

    private String mrCode;
    private String diagnoseType;
    private String notes;
    private List<Integer> icdIds;
    private Integer existingDiagnoseId;
    private List<ApotikLineItemRequest> prescriptionLines;

    public String getMrCode() {
        return mrCode;
    }

    public void setMrCode(String mrCode) {
        this.mrCode = mrCode;
    }

    public String getDiagnoseType() {
        return diagnoseType;
    }

    public void setDiagnoseType(String diagnoseType) {
        this.diagnoseType = diagnoseType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Integer> getIcdIds() {
        return icdIds;
    }

    public void setIcdIds(List<Integer> icdIds) {
        this.icdIds = icdIds;
    }

    public Integer getExistingDiagnoseId() {
        return existingDiagnoseId;
    }

    public void setExistingDiagnoseId(Integer existingDiagnoseId) {
        this.existingDiagnoseId = existingDiagnoseId;
    }

    public List<ApotikLineItemRequest> getPrescriptionLines() {
        return prescriptionLines;
    }

    public void setPrescriptionLines(List<ApotikLineItemRequest> prescriptionLines) {
        this.prescriptionLines = prescriptionLines;
    }
}
