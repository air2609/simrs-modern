package com.vone.simrs.ward;

/**
 * Hasil buat nota bed.
 */
public class BedNoteCreateResultResponse {

    private final boolean success;
    private final String message;
    private final String noteNo;

    public BedNoteCreateResultResponse(boolean success, String message, String noteNo) {
        this.success = success;
        this.message = message;
        this.noteNo = noteNo;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getNoteNo() {
        return noteNo;
    }
}
