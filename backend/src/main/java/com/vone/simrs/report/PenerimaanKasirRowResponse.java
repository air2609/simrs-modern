package com.vone.simrs.report;

/**
 * Baris laporan penerimaan kasir (RPT0012). Migrasi dari legacy
 * {@code CashierManagerImpl.getPatientSettlement()} — termasuk baris
 * subtotal "TOTAL" per kasir & baris TOTAL akhir (tgl/kasir kosong,
 * label "TOTAL" di kolom kwitansi).
 */
public class PenerimaanKasirRowResponse {

    private final String tglTransaksi;
    private final String noKwitansi;
    private final double tunai;
    private final double nonTunai;
    private final String kasir;

    public PenerimaanKasirRowResponse(String tglTransaksi, String noKwitansi, double tunai,
            double nonTunai, String kasir) {
        this.tglTransaksi = tglTransaksi;
        this.noKwitansi = noKwitansi;
        this.tunai = tunai;
        this.nonTunai = nonTunai;
        this.kasir = kasir;
    }

    public String getTglTransaksi() {
        return tglTransaksi;
    }

    public String getNoKwitansi() {
        return noKwitansi;
    }

    public double getTunai() {
        return tunai;
    }

    public double getNonTunai() {
        return nonTunai;
    }

    public String getKasir() {
        return kasir;
    }

    /** Baris total/subtotal: kolom kwitansi berisi "TOTAL", tgl & kasir kosong. */
    public boolean isTotalRow() {
        return noKwitansi != null && noKwitansi.trim().equalsIgnoreCase("TOTAL");
    }
}
