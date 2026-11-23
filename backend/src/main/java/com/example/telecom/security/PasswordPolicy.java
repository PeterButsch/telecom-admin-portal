package com.example.telecom.security;

import com.example.telecom.error.FieldValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Password rules for staff accounts. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    /** BCrypt only uses the first 72 bytes of a password. */
    static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /**
     * Throws a field validation error when the password breaks a rule.
     */
    public static void check(String field, String password, String username) {
        String problem = problem(password, username);
        if (problem != null) {
            throw new FieldValidationException(field, problem);
        }
    }

    static String problem(String password, String username) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "must be at least " + MIN_LENGTH + " characters";
        }
        if (!fitsHash(password)) {
            return "must be at most " + MAX_BYTES + " bytes";
        }
        if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            return "must contain both letters and digits";
        }
        if (username != null && password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            return "must not contain the username";
        }
        return null;
    }

    static boolean fitsHash(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
