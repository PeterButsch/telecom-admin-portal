package com.example.telecom.error;

/**
 * The request is well formed but refers to data that makes it invalid; mapped
 * to 400.
 */
public class BadRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message);
    }
}
