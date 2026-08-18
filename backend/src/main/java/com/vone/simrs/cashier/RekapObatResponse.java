package com.vone.simrs.cashier;

import java.util.List;

/**
 * Hasil rekap obat (SC0208): baris + total transaksi/retur.
 */
public class RekapObatResponse {

    private final double totalTransaksi;
    private final double totalRetur;
    private final double transaksiSetelahRetur;
    private final List<RekapObatRowResponse> rows;

    public RekapObatResponse(double totalTransaksi, double totalRetur,
            double transaksiSetelahRetur, List<RekapObatRowResponse> rows) {
        this.totalTransaksi = totalTransaksi;
        this.totalRetur = totalRetur;
        this.transaksiSetelahRetur = transaksiSetelahRetur;
        this.rows = rows;
    }

    public double getTotalTransaksi() {
        return totalTransaksi;
    }

    public double getTotalRetur() {
        return totalRetur;
    }

    public double getTransaksiSetelahRetur() {
        return transaksiSetelahRetur;
    }

    public List<RekapObatRowResponse> getRows() {
        return rows;
    }
}
