package com.vone.simrs.admission;

/**
 * Hasil aksi pendaftaran rawat inap.
 */
public class RanapActionResultResponse {

    private final boolean success;
    private final String message;

    public RanapActionResultResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
