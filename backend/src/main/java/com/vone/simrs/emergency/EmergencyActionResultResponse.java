package com.vone.simrs.emergency;

/**
 * Hasil aksi (simpan/validasi/batal nota UGD).
 */
public class EmergencyActionResultResponse {

    private final boolean success;
    private final String message;
    private final Integer noteId;
    private final String noteNo;

    public EmergencyActionResultResponse(boolean success, String message, Integer noteId,
            String noteNo) {
        this.success = success;
        this.message = message;
        this.noteId = noteId;
        this.noteNo = noteNo;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNo() {
        return noteNo;
    }
}
