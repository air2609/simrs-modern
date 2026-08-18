package com.vone.simrs.emergency;

/**
 * Baris riwayat (KETERANGAN, SUB DIVISI, TANGGAL). Migrasi dari legacy
 * {@code CommonPatientHistoryManagerImpl.getTreatmentData()}.
 */
public class EmergencyHistoryLineResponse {

    private final String keterangan;
    private final String subDivisi;
    private final String tanggal;
    private final double jumlah;

    public EmergencyHistoryLineResponse(String keterangan, String subDivisi, String tanggal,
            double jumlah) {
        this.keterangan = keterangan;
        this.subDivisi = subDivisi;
        this.tanggal = tanggal;
        this.jumlah = jumlah;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public String getSubDivisi() {
        return subDivisi;
    }

    public String getTanggal() {
        return tanggal;
    }

    public double getJumlah() {
        return jumlah;
    }
}
