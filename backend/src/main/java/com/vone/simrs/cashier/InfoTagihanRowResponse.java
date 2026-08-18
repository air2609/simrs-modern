package com.vone.simrs.cashier;

/**
 * Baris transaksi pasien (SC0023 print.zul). Migrasi dari legacy
 * {@code CashierManagerImpl.cariNotaClick()}.
 */
public class InfoTagihanRowResponse {

    private final String tanggal;
    private final String keterangan;
    private final String noteNo;
    private final String staff;
    private final String status;
    private final String kwitansi;
    private final Double jumlah;

    public InfoTagihanRowResponse(String tanggal, String keterangan, String noteNo, String staff,
            String status, String kwitansi, Double jumlah) {
        this.tanggal = tanggal;
        this.keterangan = keterangan;
        this.noteNo = noteNo;
        this.staff = staff;
        this.status = status;
        this.kwitansi = kwitansi;
        this.jumlah = jumlah;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public String getNoteNo() {
        return noteNo;
    }

    public String getStaff() {
        return staff;
    }

    public String getStatus() {
        return status;
    }

    public String getKwitansi() {
        return kwitansi;
    }

    public Double getJumlah() {
        return jumlah;
    }
}
