package com.vone.simrs.ward;

/**
 * Satu hari hunian bed (baris tree HISTORY BED PASIEN). Migrasi dari legacy
 * {@code BedTransactionManagerImpl.getBedsOccupancy()}.
 */
public class BedDayResponse {

    private final String date;
    private final String durasi;
    private final Double harga;
    private final Double hargaTotal;
    private final String noteNo;

    public BedDayResponse(String date, String durasi, Double harga, Double hargaTotal,
            String noteNo) {
        this.date = date;
        this.durasi = durasi;
        this.harga = harga;
        this.hargaTotal = hargaTotal;
        this.noteNo = noteNo;
    }

    public String getDate() {
        return date;
    }

    public String getDurasi() {
        return durasi;
    }

    public Double getHarga() {
        return harga;
    }

    public Double getHargaTotal() {
        return hargaTotal;
    }

    public String getNoteNo() {
        return noteNo;
    }
}
