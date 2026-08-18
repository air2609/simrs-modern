package com.vone.simrs.admission;

/**
 * Request batal registrasi rawat jalan.
 */
public class AdmissionRegistrationCancelRequest {

    private String registrationCode;

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }
}
