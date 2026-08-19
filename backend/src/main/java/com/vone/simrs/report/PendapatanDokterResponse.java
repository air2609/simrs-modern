package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan pendapatan dokter (RPT0013).
 *
 * <p>Untuk tipe PD/OBAT: {@code rows} terisi, {@code total} = jumlah jasa / nilai transaksi.
 * Untuk tipe ALL: {@code allRows} terisi (total tidak ditampilkan, sesuai legacy).
 */
public class PendapatanDokterResponse {

    private final String tipe;
    private final double total;
    private final List<PendapatanDokterRowResponse> rows;
    private final List<PendapatanDokterAllRowResponse> allRows;

    public PendapatanDokterResponse(String tipe, double total,
            List<PendapatanDokterRowResponse> rows,
            List<PendapatanDokterAllRowResponse> allRows) {
        this.tipe = tipe;
        this.total = total;
        this.rows = rows;
        this.allRows = allRows;
    }

    public String getTipe() {
        return tipe;
    }

    public double getTotal() {
        return total;
    }

    public List<PendapatanDokterRowResponse> getRows() {
        return rows;
    }

    public List<PendapatanDokterAllRowResponse> getAllRows() {
        return allRows;
    }
}
