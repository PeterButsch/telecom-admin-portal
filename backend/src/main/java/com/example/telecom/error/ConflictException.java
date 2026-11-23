package com.example.telecom.error;

/** The request conflicts with the current state of the data; mapped to 409. */
public class ConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
