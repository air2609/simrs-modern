package com.vone.simrs.emergency;

import java.util.List;

/**
 * Riwayat transaksi pasien (tab HISTORY TRANSAKSI). Migrasi dari legacy
 * {@code PatientHistoryController} + {@code CommonHistoryDAO.getPatientNote()}
 * (PER DIVISI / GLOBAL).
 */
public class EmergencyHistoryResponse {

    private final String mrCode;
    private final String patientName;
    private final String mode;
    private final double grandTotal;
    private final List<EmergencyHistoryNoteResponse> notes;

    public EmergencyHistoryResponse(String mrCode, String patientName, String mode,
            double grandTotal, List<EmergencyHistoryNoteResponse> notes) {
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

    public List<EmergencyHistoryNoteResponse> getNotes() {
        return notes;
    }
}
