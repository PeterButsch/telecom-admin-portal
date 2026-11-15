package com.example.telecom.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared state for one seeding run: the database, a seeded random source and
 * unique-number generators.
 */
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
final class SeedContext {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);
    private static final int LUHN_DOUBLE_LIMIT = 9;
    private static final long MSISDN_RANGE = 10_000_000L;

    private final JdbcTemplate jdbcTemplate;
    private final Random random;
    private final Instant startedAt;
    private final Set<String> msisdns = new HashSet<>();
    private final Set<String> iccids = new HashSet<>();
    private final Set<String> imeis = new HashSet<>();
    private final Set<String> accountNumbers = new HashSet<>();
    private final Set<String> identityNumbers = new HashSet<>();

    SeedContext(JdbcTemplate jdbc, long seed, Instant now) {
        this.jdbcTemplate = jdbc;
        this.random = new Random(seed);
        this.startedAt = now.truncatedTo(ChronoUnit.MILLIS);
    }

    JdbcTemplate jdbc() {
        return jdbcTemplate;
    }

    Instant now() {
        return startedAt;
    }

    LocalDate today() {
        return LocalDate.ofInstant(startedAt, ZoneOffset.UTC);
    }

    static String ts(Instant instant) {
        return instant == null ? null : TIMESTAMP.format(instant);
    }

    static String date(LocalDate date) {
        return date == null ? null : date.toString();
    }

    boolean chance(double probability) {
        return random.nextDouble() < probability;
    }

    /** Inclusive range. */
    int between(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    double between(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    /** Picks an index with the given relative weights. */
    int weighted(int... weights) {
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    Instant daysAgo(double days) {
        return startedAt.minusSeconds((long) (days * 86_400));
    }

    Instant randomBetween(Instant from, Instant to) {
        long span = Math.max(1, to.getEpochSecond() - from.getEpochSecond());
        return from.plusSeconds((long) (random.nextDouble() * span)).plusMillis(random.nextInt(1000));
    }

    String digits(int count) {
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            value.append((char) ('0' + random.nextInt(10)));
        }
        return value.toString();
    }

    /** Singapore mobile number: 8 digits starting with 8 or 9. */
    String newMsisdn() {
        String msisdn;
        do {
            msisdn = (chance(0.6) ? "9" : "8") + String.format("%07d", random.nextLong(MSISDN_RANGE));
        } while (!msisdns.add(msisdn));
        return msisdn;
    }

    /**
     * 19-digit ICCID with the Singapore country code (65) and a Luhn check digit.
     */
    String newIccid() {
        String iccid;
        do {
            String body = "896501" + digits(12);
            iccid = body + luhnDigit(body);
        } while (!iccids.add(iccid));
        return iccid;
    }

    /**
     * 15-digit IMEI: the model's 8-digit TAC, a 6-digit serial and a Luhn check
     * digit.
     */
    String newImei(String tac) {
        String imei;
        do {
            String body = tac + digits(6);
            imei = body + luhnDigit(body);
        } while (!imeis.add(imei));
        return imei;
    }

    String newAccountNumber() {
        String number;
        do {
            number = "1" + digits(9);
        } while (!accountNumbers.add(number));
        return number;
    }

    /** Reserves an identity number; false when it was already used. */
    boolean reserveIdentity(String idNumber) {
        return identityNumbers.add(idNumber);
    }

    static char luhnDigit(String body) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = body.length() - 1; i >= 0; i--) {
            int digit = body.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > LUHN_DOUBLE_LIMIT) {
                    digit -= LUHN_DOUBLE_LIMIT;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return (char) ('0' + (10 - sum % 10) % 10);
    }
}
