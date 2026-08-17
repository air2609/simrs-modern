package com.vone.simrs.mr;

/**
 * Hasil penyimpanan diagnosa (dan resep jika ada) pada screen SC0206.
 */
public class DiagnoseSaveResultResponse {

    private final Integer diagnoseId;
    private final Integer noteId;
    private final String noteNumber;

    public DiagnoseSaveResultResponse(Integer diagnoseId, Integer noteId, String noteNumber) {
        this.diagnoseId = diagnoseId;
        this.noteId = noteId;
        this.noteNumber = noteNumber;
    }

    public Integer getDiagnoseId() {
        return diagnoseId;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNumber() {
        return noteNumber;
    }
}
