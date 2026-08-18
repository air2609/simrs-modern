package com.vone.simrs.warehouse;

/**
 * Hasil kirim permintaan O-BM berisi nomor permintaan yang digenerate.
 */
public class ItemRequestSaveResultResponse {

    private final boolean success;
    private final String message;
    private final String requestCode;

    public ItemRequestSaveResultResponse(boolean success, String message, String requestCode) {
        this.success = success;
        this.message = message;
        this.requestCode = requestCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestCode() {
        return requestCode;
    }
}
