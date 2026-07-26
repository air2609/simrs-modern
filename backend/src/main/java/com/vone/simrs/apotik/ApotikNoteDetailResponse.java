package com.vone.simrs.apotik;

import java.util.List;

public class ApotikNoteDetailResponse {

    private final Integer noteId;
    private final String noteNumber;
    private final Integer statusCode;
    private final String statusLabel;
    private final double totalAmount;
    private final Integer unitId;
    private final String unitCode;
    private final String unitName;
    private final Integer patientId;
    private final Integer patientTypeId;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String address;
    private final String mrCode;
    private final Integer registrationId;
    private final String registrationCode;
    private final String receiptNumber;
    private final boolean inpatient;
    private final String tariffClass;
    private final String cancelationNote;
    private final boolean canModify;
    private final boolean canValidate;
    private final boolean canCancel;
    private final List<ApotikNoteLineResponse> lines;

    public ApotikNoteDetailResponse(
            Integer noteId, String noteNumber, Integer statusCode,
            String statusLabel, double totalAmount, Integer unitId,
            String unitCode, String unitName, Integer patientId,
            Integer patientTypeId, String patientName, String gender,
            String birthDate, String address, String mrCode,
            Integer registrationId, String registrationCode,
            String receiptNumber, boolean inpatient, String tariffClass,
            String cancelationNote, boolean canModify, boolean canValidate,
            boolean canCancel, List<ApotikNoteLineResponse> lines) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.totalAmount = totalAmount;
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.patientId = patientId;
        this.patientTypeId = patientTypeId;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.mrCode = mrCode;
        this.registrationId = registrationId;
        this.registrationCode = registrationCode;
        this.receiptNumber = receiptNumber;
        this.inpatient = inpatient;
        this.tariffClass = tariffClass;
        this.cancelationNote = cancelationNote;
        this.canModify = canModify;
        this.canValidate = canValidate;
        this.canCancel = canCancel;
        this.lines = lines;
    }

    public Integer getNoteId() { return noteId; }
    public String getNoteNumber() { return noteNumber; }
    public Integer getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public double getTotalAmount() { return totalAmount; }
    public Integer getUnitId() { return unitId; }
    public String getUnitCode() { return unitCode; }
    public String getUnitName() { return unitName; }
    public Integer getPatientId() { return patientId; }
    public Integer getPatientTypeId() { return patientTypeId; }
    public String getPatientName() { return patientName; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }
    public String getAddress() { return address; }
    public String getMedicalRecordCode() { return mrCode; }
    public Integer getRegistrationId() { return registrationId; }
    public String getRegistrationCode() { return registrationCode; }
    public String getReceiptNumber() { return receiptNumber; }
    public boolean isInpatient() { return inpatient; }
    public String getTariffClass() { return tariffClass; }
    public String getCancelationNote() { return cancelationNote; }
    public boolean isCanModify() { return canModify; }
    public boolean isCanValidate() { return canValidate; }
    public boolean isCanCancel() { return canCancel; }
    public List<ApotikNoteLineResponse> getLines() { return lines; }
}
