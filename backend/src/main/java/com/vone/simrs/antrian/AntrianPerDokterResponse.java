package com.vone.simrs.antrian;

import java.util.List;

/**
 * Hasil antrian per dokter (RPT0021 / antrianPerDokter.zul).
 */
public class AntrianPerDokterResponse {

    private final String doctorName;
    private final List<DelayAntrianQueueRowResponse> rows;

    public AntrianPerDokterResponse(String doctorName, List<DelayAntrianQueueRowResponse> rows) {
        this.doctorName = doctorName;
        this.rows = rows;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public List<DelayAntrianQueueRowResponse> getRows() {
        return rows;
    }
}
