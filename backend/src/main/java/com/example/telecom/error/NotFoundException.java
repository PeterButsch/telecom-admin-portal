package com.example.telecom.error;

/** The requested record does not exist; mapped to 404. */
public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
