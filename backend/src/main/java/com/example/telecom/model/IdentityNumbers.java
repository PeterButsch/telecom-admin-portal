package com.example.telecom.model;

import java.util.Locale;
import java.util.regex.Pattern;

/** Validation and masking for Singapore NRIC and FIN numbers. */
public final class IdentityNumbers {

    private static final Pattern NRIC_FORMAT = Pattern.compile("^[STFGM]\\d{7}[A-Z]$");
    private static final int[] WEIGHTS = {2, 7, 6, 5, 4, 3, 2};
    private static final String ST_LETTERS = "JZIHGFEDCBA";
    private static final String FG_LETTERS = "XWUTRQPNMLK";
    private static final String M_LETTERS = "KLJNPQRTUWX";
    private static final int OFFSET_T_G = 4;
    private static final int OFFSET_M = 3;
    private static final int CHECK_MODULUS = 11;
    private static final int VISIBLE_CHARACTERS = 4;

    private IdentityNumbers() {
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * True when the value is a well-formed NRIC or FIN whose check letter matches.
     */
    public static boolean isValidNricOrFin(String value) {
        String id = normalize(value);
        if (id == null || !NRIC_FORMAT.matcher(id).matches()) {
            return false;
        }
        return id.charAt(id.length() - 1) == checkLetter(id.charAt(0), id.substring(1, 8));
    }

    /**
     * True for NRIC prefixes (citizens and permanent residents); FIN prefixes are
     * F, G and M.
     */
    public static boolean isNricPrefix(String value) {
        String id = normalize(value);
        return id != null && !id.isEmpty() && (id.charAt(0) == 'S' || id.charAt(0) == 'T');
    }

    /** Computes the check letter for a prefix and seven digits. */
    public static char checkLetter(char prefix, String digits) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += Character.digit(digits.charAt(i), 10) * WEIGHTS[i];
        }
        switch (prefix) {
            case 'T', 'G' -> sum += OFFSET_T_G;
            case 'M' -> sum += OFFSET_M;
            default -> {
                // S and F use no offset.
            }
        }
        int remainder = sum % CHECK_MODULUS;
        return switch (prefix) {
            case 'S', 'T' -> ST_LETTERS.charAt(remainder);
            case 'F', 'G' -> FG_LETTERS.charAt(remainder);
            default -> M_LETTERS.charAt(CHECK_MODULUS - 1 - remainder);
        };
    }

    /**
     * Shows only the last three digits and the check letter, as PDPC guidance
     * suggests for partial NRIC display, for example "*****567D".
     */
    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        String id = normalize(value);
        if (id.length() <= VISIBLE_CHARACTERS) {
            return "*".repeat(id.length());
        }
        return "*".repeat(id.length() - VISIBLE_CHARACTERS) + id.substring(id.length() - VISIBLE_CHARACTERS);
    }
}
