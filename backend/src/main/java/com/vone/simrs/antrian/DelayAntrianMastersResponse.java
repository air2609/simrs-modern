package com.vone.simrs.antrian;

import java.util.List;

/**
 * Masters screen SCM0053: delay + text antrian + daftar dokter berantrian.
 */
public class DelayAntrianMastersResponse {

    private final Integer delayAntrian;
    private final String textAntrian;
    private final List<DelayAntrianDoctorResponse> doctors;

    public DelayAntrianMastersResponse(Integer delayAntrian, String textAntrian,
            List<DelayAntrianDoctorResponse> doctors) {
        this.delayAntrian = delayAntrian;
        this.textAntrian = textAntrian;
        this.doctors = doctors;
    }

    public Integer getDelayAntrian() {
        return delayAntrian;
    }

    public String getTextAntrian() {
        return textAntrian;
    }

    public List<DelayAntrianDoctorResponse> getDoctors() {
        return doctors;
    }
}
