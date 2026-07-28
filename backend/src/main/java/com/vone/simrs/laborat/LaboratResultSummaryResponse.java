package com.vone.simrs.laborat;

import java.time.LocalDate;

/**
 * Response untuk pencarian hasil lab (SC0043).
 */
public class LaboratResultSummaryResponse {

    private final Integer resultId;
    private final String resultCode;
    private final String noteNumber;
    private final String patientName;
    private final String mrCode;
    private final String createdAt;
    private final String createdBy;

    public LaboratResultSummaryResponse(Integer resultId, String resultCode, String noteNumber,
            String patientName, String mrCode, String createdAt, String createdBy) {
        this.resultId = resultId;
        this.resultCode = resultCode;
        this.noteNumber = noteNumber;
        this.patientName = patientName;
        this.mrCode = mrCode;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Integer getResultId() { return resultId; }
    public String getResultCode() { return resultCode; }
    public String getNoteNumber() { return noteNumber; }
    public String getPatientName() { return patientName; }
    public String getMrCode() { return mrCode; }
    public String getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
