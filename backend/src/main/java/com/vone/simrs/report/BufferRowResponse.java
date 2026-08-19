package com.vone.simrs.report;

/**
 * Baris buffer monitoring (RPT0018). Migrasi dari legacy
 * {@code MsWarehouseDAO.getItemUnderBuffer()} + {@code getOpenOpp()}.
 */
public class BufferRowResponse {

    private final String kode;
    private final String nama;
    private final String jenis;
    private final int stok;
    private final int buffer;
    private final String satuan;
    private final int openOpp;
    /** Kode PR (open OPP), utk tooltip. */
    private final String prCodes;

    public BufferRowResponse(String kode, String nama, String jenis, int stok, int buffer,
            String satuan, int openOpp, String prCodes) {
        this.kode = kode;
        this.nama = nama;
        this.jenis = jenis;
        this.stok = stok;
        this.buffer = buffer;
        this.satuan = satuan;
        this.openOpp = openOpp;
        this.prCodes = prCodes;
    }

    public String getKode() {
        return kode;
    }

    public String getNama() {
        return nama;
    }

    public String getJenis() {
        return jenis;
    }

    public int getStok() {
        return stok;
    }

    public int getBuffer() {
        return buffer;
    }

    public String getSatuan() {
        return satuan;
    }

    public int getOpenOpp() {
        return openOpp;
    }

    public String getPrCodes() {
        return prCodes;
    }
}
