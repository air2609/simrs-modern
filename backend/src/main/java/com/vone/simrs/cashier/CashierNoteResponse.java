package com.vone.simrs.cashier;

/**
 * Ringkasan nota belum lunas untuk pembayaran kasir.
 */
public class CashierNoteResponse {

    private final Integer noteId;
    private final String noteNo;
    private final String patientName;
    private final String date;
    private final Integer status;
    private final String statusLabel;
    private final Double total;

    public CashierNoteResponse(Integer noteId, String noteNo, String patientName, String date,
            Integer status, String statusLabel, Double total) {
        this.noteId = noteId;
        this.noteNo = noteNo;
        this.patientName = patientName;
        this.date = date;
        this.status = status;
        this.statusLabel = statusLabel;
        this.total = total;
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

    public String getDate() {
        return date;
    }

    public Integer getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public Double getTotal() {
        return total;
    }
}
