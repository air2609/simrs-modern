package com.vone.simrs.antrian;

/**
 * Request keluarkan pasien dari antrian (set antrian_status = 1).
 */
public class DelayAntrianTakeOutRequest {

    private Integer registrationId;

    public Integer getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }
}
