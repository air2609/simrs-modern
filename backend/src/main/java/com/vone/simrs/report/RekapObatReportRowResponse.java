package com.vone.simrs.report;

/**
 * Baris laporan rekap obat (RPT0016). Migrasi dari legacy
 * {@code NoteDAO.getRekapObatNew()} + {@code NoteManagerImpl.getRekapObat()}.
 */
public class RekapObatReportRowResponse {

    private final String namaObat;
    private final double qty;
    private final double nilaiPenjualan;

    public RekapObatReportRowResponse(String namaObat, double qty, double nilaiPenjualan) {
        this.namaObat = namaObat;
        this.qty = qty;
        this.nilaiPenjualan = nilaiPenjualan;
    }

    public String getNamaObat() {
        return namaObat;
    }

    public double getQty() {
        return qty;
    }

    public double getNilaiPenjualan() {
        return nilaiPenjualan;
    }
}
