package com.vone.simrs.antrian;

import java.util.List;

/**
 * Data layar display antrian dokter (RPT0019): nama rumah sakit, delay,
 * text berjalan, dan daftar dokter berantrian beserta antriannya.
 */
public class AntrianDisplayResponse {

    private final String hospitalName;
    private final Integer delaySeconds;
    private final String textAntrian;
    private final List<AntrianDisplayDoctorResponse> doctors;

    public AntrianDisplayResponse(String hospitalName, Integer delaySeconds, String textAntrian,
            List<AntrianDisplayDoctorResponse> doctors) {
        this.hospitalName = hospitalName;
        this.delaySeconds = delaySeconds;
        this.textAntrian = textAntrian;
        this.doctors = doctors;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public String getTextAntrian() {
        return textAntrian;
    }

    public List<AntrianDisplayDoctorResponse> getDoctors() {
        return doctors;
    }
}
