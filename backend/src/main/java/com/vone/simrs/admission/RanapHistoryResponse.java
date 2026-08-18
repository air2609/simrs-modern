package com.vone.simrs.admission;

/**
 * Baris riwayat transaksi rajal pasien (list HISTORY TRANSAKSI PASIEN).
 */
public class RanapHistoryResponse {

    private final String date;
    private final String noteNo;
    private final String description;

    public RanapHistoryResponse(String date, String noteNo, String description) {
        this.date = date;
        this.noteNo = noteNo;
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public String getNoteNo() {
        return noteNo;
    }

    public String getDescription() {
        return description;
    }
}
