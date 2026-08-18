package com.vone.simrs.ward;

import java.util.List;

/**
 * Riwayat transaksi pasien ranap (tab HISTORY TRANSAKSI).
 */
public class WardHistoryResponse {

    private final String mrCode;
    private final String patientName;
    private final String mode;
    private final double grandTotal;
    private final List<WardHistoryNoteResponse> notes;

    public WardHistoryResponse(String mrCode, String patientName, String mode, double grandTotal,
            List<WardHistoryNoteResponse> notes) {
        this.mrCode = mrCode;
        this.patientName = patientName;
        this.mode = mode;
        this.grandTotal = grandTotal;
        this.notes = notes;
    }

    public String getMrCode() {
        return mrCode;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getMode() {
        return mode;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public List<WardHistoryNoteResponse> getNotes() {
        return notes;
    }
}
