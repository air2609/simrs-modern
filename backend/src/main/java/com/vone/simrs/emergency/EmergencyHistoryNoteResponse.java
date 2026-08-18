package com.vone.simrs.emergency;

import java.util.List;

/**
 * Satu nota dalam riwayat transaksi pasien beserta barisnya.
 */
public class EmergencyHistoryNoteResponse {

    private final Integer noteId;
    private final String noteNo;
    private final String unitName;
    private final Integer status;
    private final String statusLabel;
    private final String date;
    private final double total;
    private final List<EmergencyHistoryLineResponse> lines;

    public EmergencyHistoryNoteResponse(Integer noteId, String noteNo, String unitName,
            Integer status, String statusLabel, String date, double total,
            List<EmergencyHistoryLineResponse> lines) {
        this.noteId = noteId;
        this.noteNo = noteNo;
        this.unitName = unitName;
        this.status = status;
        this.statusLabel = statusLabel;
        this.date = date;
        this.total = total;
        this.lines = lines;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getNoteNo() {
        return noteNo;
    }

    public String getUnitName() {
        return unitName;
    }

    public Integer getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getDate() {
        return date;
    }

    public double getTotal() {
        return total;
    }

    public List<EmergencyHistoryLineResponse> getLines() {
        return lines;
    }
}
