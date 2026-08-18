package com.vone.simrs.emergency;

/**
 * Ringkasan nota untuk pencarian nota (bandbox NO. NOTA). Migrasi dari legacy
 * {@code NoteDAO.searchNote()} dengan status nota AKTIF.
 */
public class EmergencyNoteSummaryResponse {

    private final Integer noteId;
    private final String noteNo;
    private final String patientName;
    private final Integer status;
    private final String statusLabel;
    private final String date;

    public EmergencyNoteSummaryResponse(Integer noteId, String noteNo, String patientName,
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
