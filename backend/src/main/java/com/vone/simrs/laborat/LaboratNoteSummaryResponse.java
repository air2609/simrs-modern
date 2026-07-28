package com.vone.simrs.laborat;

public class LaboratNoteSummaryResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final String patientName;
    private final Integer statusCode;
    private final String statusLabel;
    private final String createdAt;

    public LaboratNoteSummaryResponse(Integer noteId, String noteNumber, String patientName, Integer statusCode, String statusLabel, String createdAt) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.patientName = patientName;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.createdAt = createdAt;
    }

    public Integer getNoteId() { return noteId; }
    public String getNoteNumber() { return noteNumber; }
    public String getPatientName() { return patientName; }
    public Integer getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public String getCreatedAt() { return createdAt; }
}
