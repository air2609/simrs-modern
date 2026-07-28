package com.vone.simrs.laborat;

import java.util.List;

public class LaboratNoteDetailResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final Integer statusCode;
    private final String statusLabel;
    private final String patientName;
    private final String mrCode;
    private final String registrationCode;
    private final Integer patientTypeId;
    private final String doctorName;
    private final double totalAmount;
    private final List<LaboratNoteLineResponse> lines;

    public LaboratNoteDetailResponse(Integer noteId, String noteNumber, Integer statusCode, String statusLabel,
            String patientName, String mrCode, String registrationCode, Integer patientTypeId,
            String doctorName, double totalAmount, List<LaboratNoteLineResponse> lines) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.patientName = patientName;
        this.mrCode = mrCode;
        this.registrationCode = registrationCode;
        this.patientTypeId = patientTypeId;
        this.doctorName = doctorName;
        this.totalAmount = totalAmount;
        this.lines = lines;
    }

    public Integer getNoteId() { return noteId; }
    public String getNoteNumber() { return noteNumber; }
    public Integer getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public String getPatientName() { return patientName; }
    public String getMrCode() { return mrCode; }
    public String getRegistrationCode() { return registrationCode; }
    public Integer getPatientTypeId() { return patientTypeId; }
    public String getDoctorName() { return doctorName; }
    public double getTotalAmount() { return totalAmount; }
    public List<LaboratNoteLineResponse> getLines() { return lines; }
}
