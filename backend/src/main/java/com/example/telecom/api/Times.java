package com.example.telecom.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Formats optional dates and timestamps for JSON responses. */
public final class Times {

    /** Calendar days in filters are Singapore days, as shown in the portal. */
    public static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private Times() {
    }

    /** The first instant of the Singapore calendar day, or null for no day. */
    public static Instant startOfDay(LocalDate day) {
        return day == null ? null : day.atStartOfDay(SINGAPORE).toInstant();
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
