package com.vone.simrs.apotik;

public class ApotikReturnSummaryResponse {

    private final Integer returnId;
    private final String returnNumber;
    private final Integer originalNoteId;
    private final String originalNoteNumber;
    private final String patientName;
    private final double totalAmount;
    private final Integer statusCode;
    private final String statusLabel;
    private final String createdAt;

    public ApotikReturnSummaryResponse(
            Integer returnId, String returnNumber,
            Integer originalNoteId, String originalNoteNumber,
            String patientName, double totalAmount,
            Integer statusCode, String statusLabel, String createdAt) {
        this.returnId = returnId;
        this.returnNumber = returnNumber;
        this.originalNoteId = originalNoteId;
        this.originalNoteNumber = originalNoteNumber;
        this.patientName = patientName;
        this.totalAmount = totalAmount;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.createdAt = createdAt;
    }

    public Integer getReturnId() { return returnId; }
    public String getReturnNumber() { return returnNumber; }
    public Integer getOriginalNoteId() { return originalNoteId; }
    public String getOriginalNoteNumber() { return originalNoteNumber; }
    public String getPatientName() { return patientName; }
    public double getTotalAmount() { return totalAmount; }
    public Integer getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public String getCreatedAt() { return createdAt; }
}
