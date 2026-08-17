package com.vone.simrs.accounting;

/**
 * Baris daftar REKAP GL (SC0176). Migrasi dari legacy
 * {@code RekapGlController.getRekapList()} yang menampilkan kolom DARI,
 * SAMPAI, dan FILE (DOWNLOAD / "-").
 */
public class RekapGlRowResponse {

    private final Integer id;
    private final String from;
    private final String to;
    private final Integer status;
    private final boolean hasFile;

    public RekapGlRowResponse(Integer id, String from, String to, Integer status, boolean hasFile) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.status = status;
        this.hasFile = hasFile;
    }

    public Integer getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Integer getStatus() {
        return status;
    }

    public boolean isHasFile() {
        return hasFile;
    }
}
