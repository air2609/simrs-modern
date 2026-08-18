package com.vone.simrs.warehouse;

/**
 * Hasil aksi (kirim / setujui / batal permintaan O-BM).
 */
public class ItemRequestActionResultResponse {

    private final boolean success;
    private final String message;

    public ItemRequestActionResultResponse(boolean success, String message) {
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
