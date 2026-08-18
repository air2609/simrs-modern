package com.vone.simrs.cashier;

import java.util.List;

/**
 * Hasil informasi tagihan pasien (SC0023): baris transaksi + total.
 */
public class InfoTagihanResponse {

    private final double total;
    private final double deposit;
    private final double retur;
    private final double lunas;
    private final double sisa;
    private final List<InfoTagihanRowResponse> rows;

    public InfoTagihanResponse(double total, double deposit, double retur, double lunas,
            double sisa, List<InfoTagihanRowResponse> rows) {
        this.total = total;
        this.deposit = deposit;
        this.retur = retur;
        this.lunas = lunas;
        this.sisa = sisa;
        this.rows = rows;
    }

    public double getTotal() {
        return total;
    }

    public double getDeposit() {
        return deposit;
    }

    public double getRetur() {
        return retur;
    }

    public double getLunas() {
        return lunas;
    }

    public double getSisa() {
        return sisa;
    }

    public List<InfoTagihanRowResponse> getRows() {
        return rows;
    }
}
