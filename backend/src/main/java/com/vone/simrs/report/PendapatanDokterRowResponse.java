package com.vone.simrs.report;

/**
 * Baris laporan pendapatan dokter tipe PENDAPATAN TINDAKAN (PD) atau
 * SUMBANGSIH PENJUALAN OBAT (OBAT). Migrasi dari legacy
 * {@code NoteDAO.getPendapatanDokter()} + {@code NoteDAO.getNoteByDokter()}.
 *
 * <p>Untuk PD: nota, kode, tindakan, validasi, pasien, tipe, kelas, tanggal, jumlah=jasa.
 * Untuk OBAT: nota, pasien, tanggal, jumlah=nilai transaksi.
 */
public class PendapatanDokterRowResponse {

    private final String nota;
    private final String kode;
    private final String tindakan;
    private final String validasi;
    private final String pasien;
    private final String tipe;
    private final String kelas;
    private final String tanggal;
    private final double jumlah;

    public PendapatanDokterRowResponse(String nota, String kode, String tindakan, String validasi,
            String pasien, String tipe, String kelas, String tanggal, double jumlah) {
        this.nota = nota;
        this.kode = kode;
        this.tindakan = tindakan;
        this.validasi = validasi;
        this.pasien = pasien;
        this.tipe = tipe;
        this.kelas = kelas;
        this.tanggal = tanggal;
        this.jumlah = jumlah;
    }

    public String getNota() {
        return nota;
    }

    public String getKode() {
        return kode;
    }

    public String getTindakan() {
        return tindakan;
    }

    public String getValidasi() {
        return validasi;
    }

    public String getPasien() {
        return pasien;
    }

    public String getTipe() {
        return tipe;
    }

    public String getKelas() {
        return kelas;
    }

    public String getTanggal() {
        return tanggal;
    }

    public double getJumlah() {
        return jumlah;
    }
}
