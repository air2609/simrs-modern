package com.vone.simrs.antrian;

/**
 * Hasil aksi screen SCM0053.
 */
public class DelayAntrianActionResultResponse {

    private final boolean success;
    private final String message;

    public DelayAntrianActionResultResponse(boolean success, String message) {
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
