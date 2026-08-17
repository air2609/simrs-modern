package com.vone.simrs.antrian.apotik;

/**
 * DTO untuk satu nota pasien pada screen SCM0054 (KONTROL ANTRIAN APOTIK).
 */
public class AntrianApotikNoteResponse {

    private final Integer noteId;
    private final String noteNumber;
    private String patientName;
    private String mrCode;
    private final String createdTime;
    private final Integer patientId;
    private final Integer registrationId;

    public AntrianApotikNoteResponse(Integer noteId, String noteNumber,
            String patientName, String mrCode, String createdTime) {
        this(noteId, noteNumber, patientName, mrCode, createdTime, null, null);
    }

    public AntrianApotikNoteResponse(Integer noteId, String noteNumber,
            String patientName, String mrCode, String createdTime,
            Integer patientId, Integer registrationId) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.patientName = patientName;
        this.mrCode = mrCode;
        this.createdTime = createdTime;
        this.patientId = patientId;
        this.registrationId = registrationId;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getMrCode() {
        return mrCode;
    }

    public void setMrCode(String mrCode) {
        this.mrCode = mrCode;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }
}
