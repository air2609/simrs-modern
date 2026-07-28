package com.vone.simrs.laborat;

public class LaboratActionResultResponse {
    private final boolean success;
    private final String message;

    public LaboratActionResultResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
