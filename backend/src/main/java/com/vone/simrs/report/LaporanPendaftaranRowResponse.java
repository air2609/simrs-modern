package com.vone.simrs.report;

/**
 * Baris laporan pendaftaran (RPT0010). Migrasi dari legacy
 * {@code TbRegistrationDAO.getLaporanPendaftaran()}.
 */
public class LaporanPendaftaranRowResponse {

    private final String tanggal;
    private final String unit;
    private final Integer lakiLaki;
    private final Integer perempuan;
    private final Integer lama;
    private final Integer baru;
    private final Integer total;

    public LaporanPendaftaranRowResponse(String tanggal, String unit, Integer lakiLaki,
            Integer perempuan, Integer lama, Integer baru, Integer total) {
        this.tanggal = tanggal;
        this.unit = unit;
        this.lakiLaki = lakiLaki;
        this.perempuan = perempuan;
        this.lama = lama;
        this.baru = baru;
        this.total = total;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getLakiLaki() {
        return lakiLaki;
    }

    public Integer getPerempuan() {
        return perempuan;
    }

    public Integer getLama() {
        return lama;
    }

    public Integer getBaru() {
        return baru;
    }

    public Integer getTotal() {
        return total;
    }
}
