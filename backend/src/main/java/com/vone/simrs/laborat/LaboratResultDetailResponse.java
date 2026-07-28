package com.vone.simrs.laborat;

import java.util.List;

/**
 * Detail lengkap hasil lab termasuk header dan item-item tree (SC0043).
 */
public class LaboratResultDetailResponse {

    private final Integer resultId;
    private final String resultCode;
    private final Integer examId;
    private final String noteNumber;
    private final String patientName;
    private final String mrCode;
    private final String registrationCode;
    private final String doctorName;
    private final String hall;
    private final String bed;
    private final String takeTime;
    private final String escortDoctor;
    private final String laboratNo;
    private final List<LaboratResultItemResponse> items;
    private final boolean editable;

    public LaboratResultDetailResponse(Integer resultId, String resultCode, Integer examId,
            String noteNumber, String patientName, String mrCode, String registrationCode,
            String doctorName, String hall, String bed, String takeTime,
            String escortDoctor, String laboratNo,
            List<LaboratResultItemResponse> items, boolean editable) {
        this.resultId = resultId;
        this.resultCode = resultCode;
        this.examId = examId;
        this.noteNumber = noteNumber;
        this.patientName = patientName;
        this.mrCode = mrCode;
        this.registrationCode = registrationCode;
        this.doctorName = doctorName;
        this.hall = hall;
        this.bed = bed;
        this.takeTime = takeTime;
        this.escortDoctor = escortDoctor;
        this.laboratNo = laboratNo;
        this.items = items;
        this.editable = editable;
    }

    public Integer getResultId() { return resultId; }
    public String getResultCode() { return resultCode; }
    public Integer getExamId() { return examId; }
    public String getNoteNumber() { return noteNumber; }
    public String getPatientName() { return patientName; }
    public String getMrCode() { return mrCode; }
    public String getRegistrationCode() { return registrationCode; }
    public String getDoctorName() { return doctorName; }
    public String getHall() { return hall; }
    public String getBed() { return bed; }
    public String getTakeTime() { return takeTime; }
    public String getEscortDoctor() { return escortDoctor; }
    public String getLaboratNo() { return laboratNo; }
    public List<LaboratResultItemResponse> getItems() { return items; }
    public boolean isEditable() { return editable; }
}
