package com.vone.simrs.laborat;

public class LaboratSaveResultResponse {
    private final Integer noteId;
    private final String noteNumber;
    private final String message;

    public LaboratSaveResultResponse(Integer noteId, String noteNumber, String message) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.message = message;
    }

    public Integer getNoteId() { return noteId; }
    public String getNoteNumber() { return noteNumber; }
    public String getMessage() { return message; }
}
