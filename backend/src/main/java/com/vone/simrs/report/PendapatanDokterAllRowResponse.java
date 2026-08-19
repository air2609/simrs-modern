package com.vone.simrs.report;

/**
 * Baris laporan pendapatan dokter tipe ALL — rekapitulasi per dokter.
 * Migrasi dari legacy {@code NoteManagerImpl.getDoctorReportAll()}.
 */
public class PendapatanDokterAllRowResponse {

    private final String namaDokter;
    private final double pendapatanJasa;
    private final double sumbangsihObat;

    public PendapatanDokterAllRowResponse(String namaDokter, double pendapatanJasa,
            double sumbangsihObat) {
        this.namaDokter = namaDokter;
        this.pendapatanJasa = pendapatanJasa;
        this.sumbangsihObat = sumbangsihObat;
    }

    public String getNamaDokter() {
        return namaDokter;
    }

    public double getPendapatanJasa() {
        return pendapatanJasa;
    }

    public double getSumbangsihObat() {
        return sumbangsihObat;
    }
}
