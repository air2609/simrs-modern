package com.vone.simrs.common.api;

public class ApiErrorResponse {

    private final boolean success;
    private final String message;

    public ApiErrorResponse(String message) {
        this.success = false;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
