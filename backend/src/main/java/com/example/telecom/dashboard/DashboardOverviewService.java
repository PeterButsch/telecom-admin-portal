package com.example.telecom.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operational figures for the dashboard. Definitions:
 * <ul>
 * <li>Monthly recurring revenue (MRR) is the list price of the plan on every
 * ACTIVE line; ARPU is MRR divided by ACTIVE lines.</li>
 * <li>Consumer and enterprise are the customer type (individual or
 * business).</li>
 * <li>A line is in service from activation until termination, whatever its
 * status in between (suspended and barred lines still count).</li>
 * <li>Churn for a period is the lines terminated in it divided by the lines in
 * service at its start. Port-ins are activations that came from another
 * operator, port-outs terminations that moved to one.</li>
 * <li>Months are calendar months in Singapore time; the current month runs
 * until now.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DashboardOverviewService {

    static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    static final int MONTHS = 12;
    static final List<String> BRANDS = List.of("SINGTEL", "GOMO", "HI_CARD");
    static final List<String> OPERATORS = List.of("M1", "STARHUB", "SIMBA", "CIRCLES_LIFE", "OTHER");
    static final List<String> PRIORITIES = List.of("URGENT", "HIGH", "MEDIUM", "LOW");
    private static final Duration RECENT = Duration.ofDays(30);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    // Stored timestamps are UTC text in this format, so string comparison orders
    // them correctly.
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);
    private static final String OPEN_CASE = "status IN ('OPEN', 'IN_PROGRESS', 'PENDING_CUSTOMER')";
    private static final String MOVEMENT_SQL = """
            SELECT COALESCE(SUM(activated_at < ?1 AND (terminated_at IS NULL OR terminated_at >= ?1)), 0),
                   COALESCE(SUM(activated_at >= ?1 AND activated_at < ?2), 0),
                   COALESCE(SUM(activated_at >= ?1 AND activated_at < ?2 AND port_in_from IS NOT NULL), 0),
                   COALESCE(SUM(terminated_at >= ?1 AND terminated_at < ?2), 0),
                   COALESCE(SUM(terminated_at >= ?1 AND terminated_at < ?2 AND port_out_to IS NOT NULL), 0)
            FROM mobile_lines
            """;

    private final JdbcTemplate jdbc;

    public DashboardOverviewService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Overview overview(Instant now) {
        List<BrandFigures> brands = brandFigures();
        long activeLines = brands.stream().mapToLong(BrandFigures::active_lines).sum();
        BigDecimal revenue = brands.stream().map(BrandFigures::monthly_revenue).reduce(money(0), BigDecimal::add);
        Movement recent = movement(now.minus(RECENT), now);
        long[] resolved = jdbc.queryForObject("""
                SELECT COUNT(*), COALESCE(SUM(resolved_at <= sla_due_at), 0)
                FROM support_cases WHERE resolved_at >= ? AND resolved_at < ?
                """, (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)}, db(now.minus(RECENT)), db(now));
        long resolvedCount = resolved == null ? 0 : resolved[0];
        long metCount = resolved == null ? 0 : resolved[1];
        List<CasePriorityFigures> cases = casesByPriority(now);
        Kpis kpis = new Kpis(count("SELECT COUNT(*) FROM customers WHERE status = 'ACTIVE'"), activeLines, revenue,
                ratio(revenue, activeLines), recent.churnRate(), recent.terminations(), recent.activations(),
                recent.activations() - recent.terminations(), recent.portIns(), recent.portOuts(),
                cases.stream().mapToLong(CasePriorityFigures::open).sum(),
                cases.stream().mapToLong(CasePriorityFigures::breached).sum(), resolvedCount,
                percent(metCount, resolvedCount));
        List<MonthFigures> months = monthFigures(now);
        Instant windowStart = YearMonth.parse(months.getFirst().month()).atDay(1).atStartOfDay(SINGAPORE).toInstant();
        return new Overview(now.toString(), kpis, brands, segmentFigures(), months, porting(windowStart, now), cases);
    }

    private List<BrandFigures> brandFigures() {
        Map<String, Object[]> rows = new HashMap<>();
        jdbc.query("""
                SELECT p.brand, COUNT(*), COALESCE(SUM(p.monthly_price), 0)
                FROM mobile_lines l JOIN plans p ON p.id = l.plan_id
                WHERE l.status = 'ACTIVE' GROUP BY p.brand
                """, rs -> {
            rows.put(rs.getString(1), new Object[] {rs.getLong(2), rs.getBigDecimal(3)});
        });
        return BRANDS.stream().map(brand -> {
            Object[] row = rows.getOrDefault(brand, new Object[] {0L, BigDecimal.ZERO});
            long lines = (Long) row[0];
            BigDecimal revenue = money(row[1]);
            return new BrandFigures(brand, lines, revenue, ratio(revenue, lines));
        }).toList();
    }

    private List<SegmentFigures> segmentFigures() {
        Map<String, Object[]> lines = new HashMap<>();
        jdbc.query("""
                SELECT c.type, COUNT(*), COALESCE(SUM(p.monthly_price), 0)
                FROM mobile_lines l JOIN plans p ON p.id = l.plan_id
                JOIN accounts a ON a.id = l.account_id JOIN customers c ON c.id = a.customer_id
                WHERE l.status = 'ACTIVE' GROUP BY c.type
                """, rs -> {
            lines.put(rs.getString(1), new Object[] {rs.getLong(2), rs.getBigDecimal(3)});
        });
        Map<String, Long> customers = new HashMap<>();
        jdbc.query("SELECT type, COUNT(*) FROM customers WHERE status = 'ACTIVE' GROUP BY type", rs -> {
            customers.put(rs.getString(1), rs.getLong(2));
        });
        return List.of("CONSUMER", "ENTERPRISE").stream().map(segment -> {
            String type = "CONSUMER".equals(segment) ? "INDIVIDUAL" : "BUSINESS";
            Object[] row = lines.getOrDefault(type, new Object[] {0L, BigDecimal.ZERO});
            long count = (Long) row[0];
            BigDecimal revenue = money(row[1]);
            return new SegmentFigures(segment, customers.getOrDefault(type, 0L), count, revenue, ratio(revenue, count));
        }).toList();
    }

    private List<MonthFigures> monthFigures(Instant now) {
        YearMonth current = YearMonth.from(now.atZone(SINGAPORE));
        List<MonthFigures> result = new ArrayList<>(MONTHS);
        for (int back = MONTHS - 1; back >= 0; back--) {
            YearMonth month = current.minusMonths(back);
            Instant start = month.atDay(1).atStartOfDay(SINGAPORE).toInstant();
            Instant nextStart = month.plusMonths(1).atDay(1).atStartOfDay(SINGAPORE).toInstant();
            boolean partial = nextStart.isAfter(now);
            Movement m = movement(start, partial ? now : nextStart);
            result.add(new MonthFigures(month.toString(), partial, m.base(), m.activations(), m.terminations(),
                    m.portIns(), m.portOuts(), m.churnRate()));
        }
        return result;
    }

    private Movement movement(Instant start, Instant end) {
        return jdbc.queryForObject(MOVEMENT_SQL,
                (rs, i) -> new Movement(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)),
                db(start), db(end));
    }

    private List<OperatorFigures> porting(Instant start, Instant end) {
        Map<String, Long> ins = grouped("""
                SELECT port_in_from, COUNT(*) FROM mobile_lines
                WHERE port_in_from IS NOT NULL AND activated_at >= ? AND activated_at < ? GROUP BY port_in_from
                """, start, end);
        Map<String, Long> outs = grouped("""
                SELECT port_out_to, COUNT(*) FROM mobile_lines
                WHERE port_out_to IS NOT NULL AND terminated_at >= ? AND terminated_at < ? GROUP BY port_out_to
                """, start, end);
        return OPERATORS.stream()
                .map(op -> new OperatorFigures(op, ins.getOrDefault(op, 0L), outs.getOrDefault(op, 0L))).toList();
    }

    private List<CasePriorityFigures> casesByPriority(Instant now) {
        Map<String, long[]> rows = new HashMap<>();
        jdbc.query("SELECT priority, COUNT(*), COALESCE(SUM(sla_due_at < ?), 0) FROM support_cases WHERE " + OPEN_CASE
                + " GROUP BY priority", rs -> {
                    rows.put(rs.getString(1), new long[] {rs.getLong(2), rs.getLong(3)});
                }, db(now));
        return PRIORITIES.stream().map(p -> {
            long[] row = rows.getOrDefault(p, new long[2]);
            return new CasePriorityFigures(p, row[0], row[1]);
        }).toList();
    }

    private Map<String, Long> grouped(String sql, Instant start, Instant end) {
        Map<String, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getString(1), rs.getLong(2));
        }, db(start), db(end));
        return result;
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    static String db(Instant instant) {
        return DB_TIME.format(instant);
    }

    private static BigDecimal money(Object value) {
        BigDecimal amount = value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(BigDecimal amount, long count) {
        return count == 0 ? null : amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    /**
     * A percentage with two decimals, or null when there is nothing to divide by.
     */
    static BigDecimal percent(long part, long whole) {
        return whole == 0
                ? null
                : BigDecimal.valueOf(part).multiply(HUNDRED).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    private record Movement(long base, long activations, long portIns, long terminations, long portOuts) {
        BigDecimal churnRate() {
            return percent(terminations, base);
        }
    }

    public record Overview(String as_of, Kpis kpis, List<BrandFigures> brands, List<SegmentFigures> segments,
            List<MonthFigures> months, List<OperatorFigures> porting, List<CasePriorityFigures> open_cases) {
        public Overview {
            brands = List.copyOf(brands);
            segments = List.copyOf(segments);
            months = List.copyOf(months);
            porting = List.copyOf(porting);
            open_cases = List.copyOf(open_cases);
        }
    }

    /** Headline figures; the *_30d figures cover the 30 days up to now. */
    public record Kpis(long active_customers, long active_lines, BigDecimal monthly_revenue, BigDecimal arpu,
            BigDecimal churn_rate_30d, long terminations_30d, long activations_30d, long net_adds_30d,
            long port_ins_30d, long port_outs_30d, long open_cases, long breached_open_cases, long resolved_30d,
            BigDecimal sla_met_rate_30d) {
    }

    public record BrandFigures(String brand, long active_lines, BigDecimal monthly_revenue, BigDecimal arpu) {
    }

    public record SegmentFigures(String segment, long active_customers, long active_lines, BigDecimal monthly_revenue,
            BigDecimal arpu) {
    }

    public record MonthFigures(String month, boolean partial, long lines_at_start, long activations, long terminations,
            long port_ins, long port_outs, BigDecimal churn_rate) {
    }

    public record OperatorFigures(String operator, long port_ins, long port_outs) {
    }

    public record CasePriorityFigures(String priority, long open, long breached) {
    }
}
