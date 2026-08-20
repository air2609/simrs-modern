package com.vone.simrs.master.patient;

/**
 * Hasil simpan/ubah data pasien (SCM0011).
 */
public class PatientSaveResultResponse {

    private final boolean modify;
    private final String message;
    private final String mrCode;

    public PatientSaveResultResponse(boolean modify, String message, String mrCode) {
        this.modify = modify;
        this.message = message;
        this.mrCode = mrCode;
    }

    public boolean isModify() {
        return modify;
    }

    public String getMessage() {
        return message;
    }

    public String getMrCode() {
        return mrCode;
    }
}
