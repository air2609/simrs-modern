package com.vone.simrs.ward;

/**
 * Hasil aksi nota bangsal (simpan/ubah/validasi/batal).
 */
public class WardActionResultResponse {

    private final boolean success;
    private final String message;
    private final Integer noteId;
    private final String noteNo;

    public WardActionResultResponse(boolean success, String message, Integer noteId, String noteNo) {
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
