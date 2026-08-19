package com.vone.simrs.report;

/**
 * Baris laporan persediaan obat-bahan medis (RPT0008). Migrasi dari legacy
 * {@code ItemInventoryDAO.getLaporanInventory()} — fungsi database
 * {@code report.laporan_persediaan_obat(warehouseid)}.
 */
public class LaporanPersediaanRowResponse {

    private final Integer nomor;
    private final String kodeObat;
    private final String namaObat;
    private final Double hargaStandar;
    private final Double jumlah;
    private final String satuan;

    public LaporanPersediaanRowResponse(Integer nomor, String kodeObat, String namaObat,
            Double hargaStandar, Double jumlah, String satuan) {
        this.nomor = nomor;
        this.kodeObat = kodeObat;
        this.namaObat = namaObat;
        this.hargaStandar = hargaStandar;
        this.jumlah = jumlah;
        this.satuan = satuan;
    }

    public Integer getNomor() {
        return nomor;
    }

    public String getKodeObat() {
        return kodeObat;
    }

    public String getNamaObat() {
        return namaObat;
    }

    public Double getHargaStandar() {
        return hargaStandar;
    }

    public Double getJumlah() {
        return jumlah;
    }

    public String getSatuan() {
        return satuan;
    }
}
