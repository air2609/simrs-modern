package com.vone.simrs.report;

/**
 * Baris laporan penjualan pasien (RPT0001). Migrasi dari legacy
 * {@code ItemDAO.getRajalReport()} / {@code ItemDAO.getRanapReport()}.
 *
 * <p>Kolom gabungan hasil fungsi database {@code report.laporan_penjualan_rajal}
 * / {@code report.laporan_penjualan_ranap}. Untuk tipe RAWAT JALAN hanya kolom
 * no/nota/pasien/total yang terisi, untuk RAWAT INAP semua kolom terisi.
 */
public class PenjualanReportRowResponse {

    private final Integer no;
    private final String nota;
    private final String noResep;
    private final String pasien;
    private final String reg;
    private final String bed;
    private final String ruangan;
    private final Integer r;
    private final Double total;
    private final Double diskon;
    private final Double ppn;
    private final Double totalAkhir;
    private final String grup;

    public PenjualanReportRowResponse(Integer no, String nota, String noResep, String pasien,
            String reg, String bed, String ruangan, Integer r, Double total, Double diskon,
            Double ppn, Double totalAkhir, String grup) {
        this.no = no;
        this.nota = nota;
        this.noResep = noResep;
        this.pasien = pasien;
        this.reg = reg;
        this.bed = bed;
        this.ruangan = ruangan;
        this.r = r;
        this.total = total;
        this.diskon = diskon;
        this.ppn = ppn;
        this.totalAkhir = totalAkhir;
        this.grup = grup;
    }

    public Integer getNo() {
        return no;
    }

    public String getNota() {
        return nota;
    }

    public String getNoResep() {
        return noResep;
    }

    public String getPasien() {
        return pasien;
    }

    public String getReg() {
        return reg;
    }

    public String getBed() {
        return bed;
    }

    public String getRuangan() {
        return ruangan;
    }

    public Integer getR() {
        return r;
    }

    public Double getTotal() {
        return total;
    }

    public Double getDiskon() {
        return diskon;
    }

    public Double getPpn() {
        return ppn;
    }

    public Double getTotalAkhir() {
        return totalAkhir;
    }

    public String getGrup() {
        return grup;
    }
}
