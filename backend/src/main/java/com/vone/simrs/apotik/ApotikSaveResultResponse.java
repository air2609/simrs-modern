package com.vone.simrs.apotik;

public class ApotikSaveResultResponse {

    private final Integer noteId;
    private final String noteNumber;

    public ApotikSaveResultResponse(Integer noteId, String noteNumber) {
        this.noteId = noteId;
        this.noteNumber = noteNumber;
    }

    public Integer getNoteId() { return noteId; }
    public String getNoteNumber() { return noteNumber; }
}
