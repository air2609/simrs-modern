package com.vone.simrs.report;

/**
 * Baris laporan BOR (SC0073). Migrasi dari legacy
 * {@code MsBedDAO.getBorInfo()} + {@code BedManagerImpl.getBorReport()}.
 */
public class BorReportRowResponse {

    private final String kelas;
    private final String ruangan;
    private final int totalBed;
    private final int totalTerisi;
    /** Nilai BOR dalam persen (sudah dikali 100). */
    private final double bor;

    public BorReportRowResponse(String kelas, String ruangan, int totalBed, int totalTerisi,
            double bor) {
        this.kelas = kelas;
        this.ruangan = ruangan;
        this.totalBed = totalBed;
        this.totalTerisi = totalTerisi;
        this.bor = bor;
    }

    public String getKelas() {
        return kelas;
    }

    public String getRuangan() {
        return ruangan;
    }

    public int getTotalBed() {
        return totalBed;
    }

    public int getTotalTerisi() {
        return totalTerisi;
    }

    public double getBor() {
        return bor;
    }
}
