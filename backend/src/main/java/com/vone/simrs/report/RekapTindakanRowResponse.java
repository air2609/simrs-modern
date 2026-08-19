package com.vone.simrs.report;

/**
 * Baris laporan rekap tindakan (RPT0011). Migrasi dari legacy
 * {@code MsTreatmentDAO.getTreatmentReport()}.
 */
public class RekapTindakanRowResponse {

    private final String kode;
    private final String namaTindakan;
    private final double qty;
    private final double total;

    public RekapTindakanRowResponse(String kode, String namaTindakan, double qty, double total) {
        this.kode = kode;
        this.namaTindakan = namaTindakan;
        this.qty = qty;
        this.total = total;
    }

    public String getKode() {
        return kode;
    }

    public String getNamaTindakan() {
        return namaTindakan;
    }

    public double getQty() {
        return qty;
    }

    public double getTotal() {
        return total;
    }
}
