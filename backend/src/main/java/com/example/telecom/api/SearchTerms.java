package com.example.telecom.api;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns a free-text search into LIKE patterns: a lower-case text pattern, and,
 * when the query looks like a phone or account number, a digits-only pattern
 * (so "+65 9123 4567" matches 91234567). Queries with letters, such as ID
 * numbers, never become number searches. Text patterns escape LIKE wildcards,
 * so queries using them must add {@code ESCAPE '!'}.
 */
public record SearchTerms(String text, String digits) {

    /**
     * The escape character text patterns use; queries must say {@code ESCAPE '!'}.
     */
    public static final String ESCAPE = "!";
    private static final int MIN_DIGITS = 3;
    private static final String SINGAPORE_PREFIX = "65";
    private static final int LOCAL_NUMBER_LENGTH = 8;
    private static final Pattern NUMBER_LIKE = Pattern.compile("^[+0-9 ()-]+$");

    public static SearchTerms of(String query) {
        if (query == null || query.isBlank()) {
            return new SearchTerms(null, null);
        }
        String trimmed = query.trim();
        String text = "%" + escape(trimmed.toLowerCase(Locale.ROOT)) + "%";
        if (!NUMBER_LIKE.matcher(trimmed).matches()) {
            return new SearchTerms(text, null);
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() == LOCAL_NUMBER_LENGTH + SINGAPORE_PREFIX.length() && digits.startsWith(SINGAPORE_PREFIX)) {
            digits = digits.substring(SINGAPORE_PREFIX.length());
        }
        return new SearchTerms(text, digits.length() >= MIN_DIGITS ? "%" + digits + "%" : null);
    }

    /** Escapes LIKE wildcards with {@link #ESCAPE}, so % and _ match themselves. */
    private static String escape(String value) {
        return value.replace(ESCAPE, ESCAPE + ESCAPE).replace("%", ESCAPE + "%").replace("_", ESCAPE + "_");
    }
}
