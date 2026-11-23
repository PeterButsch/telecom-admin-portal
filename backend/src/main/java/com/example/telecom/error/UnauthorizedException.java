package com.example.telecom.error;

/** Sign-in failed or the session is no longer valid; mapped to 401. */
public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
