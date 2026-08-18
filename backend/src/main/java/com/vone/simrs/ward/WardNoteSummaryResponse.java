package com.vone.simrs.ward;

/**
 * Ringkasan nota bangsal (bandbox NO. NOTA). Migrasi dari
 * {@code NoteManagerImpl.searchNote()} (nota AKTIF).
 */
public class WardNoteSummaryResponse {

    private final Integer noteId;
    private final String noteNo;
    private final String patientName;
    private final Integer status;
    private final String statusLabel;
    private final String date;

    public WardNoteSummaryResponse(Integer noteId, String noteNo, String patientName,
            Integer status, String statusLabel, String date) {
        this.noteId = noteId;
        this.noteNo = noteNo;
        this.patientName = patientName;
        this.status = status;
        this.statusLabel = statusLabel;
        this.date = date;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNo() {
        return noteNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public Integer getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getDate() {
        return date;
    }
}
