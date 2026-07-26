package com.vone.simrs.apotik;

import java.util.List;

public class ApotikReturnDetailResponse {

    private final Integer returnId;
    private final String returnNumber;
    private final Integer originalNoteId;
    private final String originalNoteNumber;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String address;
    private final String mrCode;
    private final double totalAmount;
    private final Integer statusCode;
    private final String statusLabel;
    private final String cancelationNote;
    private final boolean canModify;
    private final boolean canValidate;
    private final boolean canCancel;
    private final List<ApotikReturnLineResponse> lines;

    public ApotikReturnDetailResponse(
            Integer returnId, String returnNumber,
            Integer originalNoteId, String originalNoteNumber,
            String patientName, String gender, String birthDate,
            String address, String mrCode, double totalAmount,
            Integer statusCode, String statusLabel,
            String cancelationNote, boolean canModify,
            boolean canValidate, boolean canCancel,
            List<ApotikReturnLineResponse> lines) {
        this.returnId = returnId;
        this.returnNumber = returnNumber;
        this.originalNoteId = originalNoteId;
        this.originalNoteNumber = originalNoteNumber;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.mrCode = mrCode;
        this.totalAmount = totalAmount;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.cancelationNote = cancelationNote;
        this.canModify = canModify;
        this.canValidate = canValidate;
        this.canCancel = canCancel;
        this.lines = lines;
    }

    public Integer getReturnId() { return returnId; }
    public String getReturnNumber() { return returnNumber; }
    public Integer getOriginalNoteId() { return originalNoteId; }
    public String getOriginalNoteNumber() { return originalNoteNumber; }
    public String getPatientName() { return patientName; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }
    public String getAddress() { return address; }
    public String getMedicalRecordCode() { return mrCode; }
    public double getTotalAmount() { return totalAmount; }
    public Integer getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public String getCancelationNote() { return cancelationNote; }
    public boolean isCanModify() { return canModify; }
    public boolean isCanValidate() { return canValidate; }
    public boolean isCanCancel() { return canCancel; }
    public List<ApotikReturnLineResponse> getLines() { return lines; }
}
