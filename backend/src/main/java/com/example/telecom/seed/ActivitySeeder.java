package com.example.telecom.seed;

import com.example.telecom.model.SupportCase;
import com.example.telecom.seed.CustomerSeeder.SeedCustomer;
import com.example.telecom.seed.CustomerSeeder.SeedLine;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Seeds handsets, daily usage and support cases for the seeded lines. */
@SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity",
        "PMD.NullAssignment", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
final class ActivitySeeder {

    static final int USAGE_DAYS = 60;
    static final int CASES = 520;
    private static final int CASE_WINDOW_DAYS = 150;
    /** Cases opened this recently may still be in the work queue. */
    private static final int RECENT_DAYS = 14;
    private static final int BATCH = 10_000;
    private static final int UNLIMITED_DAILY_MB = 4000;
    private static final String ACTIVE = "ACTIVE";

    private final SeedContext ctx;

    ActivitySeeder(SeedContext ctx) {
        this.ctx = ctx;
    }

    /** Returns the number of usage rows written. */
    int seed(List<SeedCustomer> customers) {
        List<SeedLine> lines = customers.stream().flatMap(c -> c.lines().stream()).toList();
        seedDevices(lines);
        int usage = seedUsage(lines);
        seedCases(customers);
        return usage;
    }

    private void seedDevices(List<SeedLine> lines) {
        List<Object[]> rows = new ArrayList<>();
        for (SeedLine line : lines) {
            boolean live = ACTIVE.equals(line.status()) || "SUSPENDED".equals(line.status())
                    || "BARRED".equals(line.status());
            boolean handsetPlan = line.plan().contractMonths() > 0 && !line.plan().dataOnly();
            if (live && handsetPlan && (!line.business() || ctx.chance(0.7))) {
                SgReferenceData.DeviceModel model = ctx.pick(SgReferenceData.DEVICE_MODELS);
                rows.add(new Object[] {ctx.newImei(model.tac()), model.manufacturer(), model.model(), "ASSIGNED",
                        line.id(), SeedContext.ts(line.activatedAt()),
                        SeedContext.ts(line.activatedAt().minusSeconds(ctx.between(3, 30) * 86_400L))});
            }
        }
        addStock(rows, "AVAILABLE", 260);
        addStock(rows, "DAMAGED", 22);
        addStock(rows, "LOST", 9);
        ctx.jdbc().batchUpdate("INSERT INTO devices (imei, manufacturer, model, status, line_id, assigned_at,"
                + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", rows);
    }

    private void addStock(List<Object[]> rows, String status, int count) {
        for (int i = 0; i < count; i++) {
            SgReferenceData.DeviceModel model = ctx.pick(SgReferenceData.DEVICE_MODELS);
            rows.add(new Object[] {ctx.newImei(model.tac()), model.manufacturer(), model.model(), status, null, null,
                    SeedContext.ts(ctx.daysAgo(ctx.between(5, 400)))});
        }
    }

    private int seedUsage(List<SeedLine> lines) {
        String insert = "INSERT INTO usage_records (line_id, type, quantity, cost, roaming, recorded_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> rows = new ArrayList<>(BATCH);
        int total = 0;
        LocalDate today = ctx.today();
        LocalDate windowStart = today.minusDays(USAGE_DAYS - 1L);
        for (SeedLine line : lines) {
            boolean used = ACTIVE.equals(line.status()) || line.terminatedAt() != null;
            if (!used || line.activatedAt() == null) {
                continue;
            }
            LocalDate from = max(windowStart, LocalDate.ofInstant(line.activatedAt(), ZoneOffset.UTC));
            LocalDate to = line.terminatedAt() != null
                    ? LocalDate.ofInstant(line.terminatedAt(), ZoneOffset.UTC)
                    : today;
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                addDay(rows, line, day);
                if (rows.size() >= BATCH) {
                    ctx.jdbc().batchUpdate(insert, rows);
                    total += rows.size();
                    rows.clear();
                }
            }
        }
        ctx.jdbc().batchUpdate(insert, rows);
        return total + rows.size();
    }

    private void addDay(List<Object[]> rows, SeedLine line, LocalDate day) {
        Instant dayStart = later(day.atStartOfDay().toInstant(ZoneOffset.UTC), line.activatedAt());
        Instant latest = day.equals(ctx.today())
                ? ctx.now()
                : day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1);
        if (line.terminatedAt() != null && line.terminatedAt().isBefore(latest)) {
            latest = line.terminatedAt();
        }
        boolean roaming = ctx.chance(0.02);
        if (ctx.chance(0.95)) {
            int megabytes = dailyMegabytes(line);
            double cost = roaming ? Math.round(megabytes * 0.5) / 100.0 : 0.0;
            rows.add(row(line, "DATA", megabytes, cost, roaming, dayStart, latest));
        }
        if (line.plan().dataOnly()) {
            return;
        }
        if (ctx.chance(0.75)) {
            int minutes = ctx.between(1, 90);
            rows.add(row(line, "CALL", minutes, roaming ? minutes * 0.39 : 0.0, roaming, dayStart, latest));
        }
        if (ctx.chance(0.35)) {
            rows.add(row(line, "SMS", ctx.between(1, 12), 0.0, false, dayStart, latest));
        }
    }

    private int dailyMegabytes(SeedLine line) {
        Integer gb = line.plan().dataGb();
        if (gb == null) {
            return ctx.between(500, UNLIMITED_DAILY_MB);
        }
        int dailyBudget = Math.max(2, gb * 1024 / 30);
        return ctx.between(1, Math.min(UNLIMITED_DAILY_MB, dailyBudget * 13 / 10));
    }

    private Object[] row(SeedLine line, String type, double quantity, double cost, boolean roaming, Instant from,
            Instant to) {
        return new Object[] {line.id(), type, quantity, Math.round(cost * 100) / 100.0, roaming ? 1 : 0,
                SeedContext.ts(ctx.randomBetween(from, to))};
    }

    private void seedCases(List<SeedCustomer> customers) {
        List<SeedCustomer> eligible = customers.stream().filter(c -> !c.lines().isEmpty()).toList();
        List<Object[]> rows = new ArrayList<>();
        for (int i = 1; i <= CASES; i++) {
            SeedCustomer customer = ctx.pick(eligible);
            SgReferenceData.CaseTemplate template = ctx.pick(SgReferenceData.CASE_TEMPLATES);
            boolean accountLevel = "BILLING".equals(template.category()) || "GENERAL".equals(template.category());
            Long lineId = accountLevel ? null : ctx.pick(customer.lines()).id();
            String priority = new String[] {"LOW", "MEDIUM", "HIGH", "URGENT"}[ctx.weighted(20, 50, 24, 6)];
            if ("SCAM_REPORT".equals(template.category()) && ("LOW".equals(priority) || "MEDIUM".equals(priority))) {
                priority = "HIGH";
            }
            int slaHours = SupportCase.Priority.valueOf(priority).slaHours();
            Instant created = ctx.randomBetween(ctx.daysAgo(CASE_WINDOW_DAYS), ctx.daysAgo(0.02));
            double slaShare = ctx.chance(0.85) ? ctx.between(0.1, 0.95) : ctx.between(1.05, 2.5);
            Instant resolved = created.plusSeconds((long) (slaHours * 3600 * slaShare));
            boolean recent = created.isAfter(ctx.daysAgo(RECENT_DAYS));
            String status;
            if (resolved.isBefore(ctx.now()) && !(recent && ctx.chance(0.4))) {
                boolean closed = resolved.plus(3, ChronoUnit.DAYS).isBefore(ctx.now()) && ctx.chance(0.4);
                status = closed ? "CLOSED" : "RESOLVED";
            } else {
                resolved = null;
                status = new String[] {"OPEN", "IN_PROGRESS", "PENDING_CUSTOMER"}[ctx.weighted(40, 45, 15)];
            }
            rows.add(new Object[] {i, "CS" + (1_000_000 + i), customer.id(), lineId, template.category(),
                    new String[] {"PHONE", "CHAT", "APP", "STORE", "EMAIL"}[ctx.weighted(35, 30, 15, 12, 8)],
                    template.subject(), template.description(), priority, status,
                    SeedContext.ts(created.plus(slaHours, ChronoUnit.HOURS)), SeedContext.ts(created),
                    SeedContext.ts(resolved)});
        }
        ctx.jdbc()
                .batchUpdate("INSERT INTO support_cases (id, case_number, customer_id, line_id, category, channel,"
                        + " subject, description, priority, status, sla_due_at, created_at, resolved_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows);
    }

    private static Instant later(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
