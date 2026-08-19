package com.vone.simrs.report;

import java.util.List;

/**
 * Hasil laporan pasien masuk bangsal (RPT0005).
 */
public class PasienBangsalResponse {

    private final String wardName;
    private final List<PasienBangsalRowResponse> rows;

    public PasienBangsalResponse(String wardName, List<PasienBangsalRowResponse> rows) {
        this.wardName = wardName;
        this.rows = rows;
    }

    public String getWardName() {
        return wardName;
    }

    public List<PasienBangsalRowResponse> getRows() {
        return rows;
    }
}
