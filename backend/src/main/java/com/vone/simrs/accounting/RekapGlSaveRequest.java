package com.vone.simrs.accounting;

/**
 * Request simpan REKAP GL (SC0176): rentang tanggal DARI-SAMPAI. Migrasi dari
 * legacy {@code RekapGlController.save()}.
 */
public class RekapGlSaveRequest {

    private String from; // ISO yyyy-MM-dd
    private String to; // ISO yyyy-MM-dd

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
