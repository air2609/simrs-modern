package com.vone.simrs.admission;

/**
 * Hasil simpan pendaftaran rawat inap.
 */
public class RanapSaveResultResponse {

    private final boolean success;
    private final String message;
    private final String registrationNo;
    private final String registrationDate;
    private final Integer ranapCount;

    public RanapSaveResultResponse(boolean success, String message, String registrationNo,
            String registrationDate, Integer ranapCount) {
        this.success = success;
        this.message = message;
        this.registrationNo = registrationNo;
        this.registrationDate = registrationDate;
        this.ranapCount = ranapCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public Integer getRanapCount() {
        return ranapCount;
    }
}
