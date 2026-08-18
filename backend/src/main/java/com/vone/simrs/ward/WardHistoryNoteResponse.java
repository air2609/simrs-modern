package com.vone.simrs.ward;

import java.util.List;

/**
 * Satu nota dalam riwayat pasien ranap beserta barisnya.
 */
public class WardHistoryNoteResponse {

    private final Integer noteId;
    private final String noteNo;
    private final String unitName;
    private final Integer status;
    private final String statusLabel;
    private final String date;
    private final double total;
    private final List<WardHistoryLineResponse> lines;

    public WardHistoryNoteResponse(Integer noteId, String noteNo, String unitName, Integer status,
            String statusLabel, String date, double total, List<WardHistoryLineResponse> lines) {
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

    public List<WardHistoryLineResponse> getLines() {
        return lines;
    }
}
