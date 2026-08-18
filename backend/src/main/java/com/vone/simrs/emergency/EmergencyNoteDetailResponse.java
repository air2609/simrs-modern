package com.vone.simrs.emergency;

import java.util.List;

/**
 * Detail nota UGD. Migrasi dari legacy {@code EmergencyManagerImpl.getNoteDetil()}
 * + {@code NoteManagerImpl.getNoteDetil()}.
 */
public class EmergencyNoteDetailResponse {

    private final Integer noteId;
    private final String noteNo;
    private final Integer status;
    private final String statusLabel;
    private final Double total;
    private final Integer unitId;
    private final String unitName;
    private final Integer patientId;
    private final String mrCode;
    private final String patientName;
    private final String gender;
    private final String birthDate;
    private final String age;
    private final String address;
    private final Integer patientTypeId;
    private final Integer escortId;
    private final Integer registrationId;
    private final String registrationNumber;
    private final Integer doctorId;
    private final String doctorName;
    private final String cancelationNote;
    private final boolean canModify;
    private final boolean canValidate;
    private final boolean canCancel;
    private final List<EmergencyNoteLineResponse> lines;

    public EmergencyNoteDetailResponse(Integer noteId, String noteNo, Integer status,
            String statusLabel, Double total, Integer unitId, String unitName, Integer patientId,
            String mrCode, String patientName, String gender, String birthDate, String age,
            String address, Integer patientTypeId, Integer escortId, Integer registrationId,
            String registrationNumber, Integer doctorId, String doctorName,
            String cancelationNote, boolean canModify, boolean canValidate, boolean canCancel,
            List<EmergencyNoteLineResponse> lines) {
        this.noteId = noteId;
        this.noteNo = noteNo;
        this.status = status;
        this.statusLabel = statusLabel;
        this.total = total;
        this.unitId = unitId;
        this.unitName = unitName;
        this.patientId = patientId;
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.age = age;
        this.address = address;
        this.patientTypeId = patientTypeId;
        this.escortId = escortId;
        this.registrationId = registrationId;
        this.registrationNumber = registrationNumber;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.cancelationNote = cancelationNote;
        this.canModify = canModify;
        this.canValidate = canValidate;
        this.canCancel = canCancel;
        this.lines = lines;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNo() {
        return noteNo;
    }

    public Integer getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public Double getTotal() {
        return total;
    }

    public Integer getUnitId() {
        return unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAge() {
        return age;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPatientTypeId() {
        return patientTypeId;
    }

    public Integer getEscortId() {
        return escortId;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getCancelationNote() {
        return cancelationNote;
    }

    public boolean isCanModify() {
        return canModify;
    }

    public boolean isCanValidate() {
        return canValidate;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public List<EmergencyNoteLineResponse> getLines() {
        return lines;
    }
}
