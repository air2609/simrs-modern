package com.vone.simrs.satusehat;

/**
 * Exception untuk kegagalan komunikasi/validasi dengan SATUSEHAT.
 */
public class SatuSehatException extends RuntimeException {

    public SatuSehatException(String message) {
        super(message);
    }

    public SatuSehatException(String message, Throwable cause) {
        super(message, cause);
    }
}
