package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan harian bangsal (RPT0006) — informasi pasien + rincian
 * transaksi + baris total (keterangan "T  O  T  A  L"), sesuai legacy.
 */
public class LaporanHarianBangsalResponse {

    private final String mrNo;
    private final String namaPasien;
    private final String regNo;
    private final String bed;
    private final String ruangan;
    private final String kelas;
    private final double totalNilai;
    private final List<LaporanHarianBangsalRowResponse> rows;

    public LaporanHarianBangsalResponse(String mrNo, String namaPasien, String regNo,
            String bed, String ruangan, String kelas, double totalNilai,
            List<LaporanHarianBangsalRowResponse> rows) {
        this.mrNo = mrNo;
        this.namaPasien = namaPasien;
        this.regNo = regNo;
        this.bed = bed;
        this.ruangan = ruangan;
        this.kelas = kelas;
        this.totalNilai = totalNilai;
        this.rows = rows;
    }

    public String getMrNo() {
        return mrNo;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public String getRegNo() {
        return regNo;
    }

    public String getBed() {
        return bed;
    }

    public String getRuangan() {
        return ruangan;
    }

    public String getKelas() {
        return kelas;
    }

    public double getTotalNilai() {
        return totalNilai;
    }

    public List<LaporanHarianBangsalRowResponse> getRows() {
        return rows;
    }
}
