package com.example.telecom.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Formats optional dates and timestamps for JSON responses. */
public final class Times {

    private Times() {
    }

    /** The current time at the millisecond precision timestamps are stored with. */
    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    public static String iso(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
