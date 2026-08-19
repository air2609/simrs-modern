package com.vone.simrs.report;

/**
 * Opsi bangsal (LOKASI) pada screen RPT0005. Bangsal diambil dari ms_ward
 * karena laporan legacy memfilter berdasarkan {@code ms_ward.v_ward_name}.
 */
public class WardOptionResponse {

    private final Integer wardId;
    private final String wardName;

    public WardOptionResponse(Integer wardId, String wardName) {
        this.wardId = wardId;
        this.wardName = wardName;
    }

    public Integer getWardId() {
        return wardId;
    }

    public String getWardName() {
        return wardName;
    }
}
