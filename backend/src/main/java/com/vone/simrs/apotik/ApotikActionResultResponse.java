package com.vone.simrs.apotik;

public class ApotikActionResultResponse {

    private final boolean success;
    private final String message;

    public ApotikActionResultResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
