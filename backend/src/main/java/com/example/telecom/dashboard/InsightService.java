package com.example.telecom.dashboard;

import com.example.telecom.security.Permission;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alerts about usage and plans. Checks:
 * <ul>
 * <li>Roaming charges on a line in the 7 days up to the latest usage record:
 * S$50 or more is a high alert for that line; S$20 to S$50 is one medium alert
 * listing all such lines. The window ends at the latest record rather than now
 * because usage arrives from mediation in batches, so the most recent hours can
 * be missing.</li>
 * <li>Active lines on a retired plan: one medium alert per plan.</li>
 * <li>Contracts ending in the next 30 days (medium) and active lines whose
 * contract has already ended (low).</li>
 * <li>Promotional discounts ending in the next 30 days (medium).</li>
 * </ul>
 * Dates are Singapore calendar days; "active" means line status ACTIVE.
 */
@Service
@Transactional(readOnly = true)
public class InsightService {

    static final BigDecimal ROAMING_HIGH = new BigDecimal("50.00");
    static final BigDecimal ROAMING_MEDIUM = new BigDecimal("20.00");
    static final Duration ROAMING_WINDOW = Duration.ofDays(7);
    static final int ENDING_DAYS = 30;
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);
    private static final String LINE_SELECT = """
            SELECT l.id, l.msisdn, c.id,
            CASE WHEN c.type = 'BUSINESS' THEN c.company_name ELSE c.first_name || ' ' || c.last_name END,
            p.name""";
    private static final String LINE_JOINS = """
            JOIN plans p ON p.id = l.plan_id JOIN accounts a ON a.id = l.account_id
            JOIN customers c ON c.id = a.customer_id""";
    /**
     * Lines with a date column and a note column, filtered by the WHERE clause that
     * follows.
     */
    private static final String DATED_LINES = " FROM mobile_lines l " + LINE_JOINS
            + " LEFT JOIN promotions pr ON pr.id = l.promotion_id WHERE ";
    private static final String RETIRED_PLAN_LINES = LINE_SELECT + ", l.activated_at, NULL" + DATED_LINES
            + "l.plan_id = ? AND l.status = 'ACTIVE' ORDER BY l.activated_at, l.id";
    private static final String CONTRACTS_ENDING = LINE_SELECT + ", l.contract_end, NULL" + DATED_LINES
            + "l.status = 'ACTIVE' AND l.contract_end >= ? AND l.contract_end <= ? ORDER BY l.contract_end, l.id";
    private static final String OUT_OF_CONTRACT = LINE_SELECT + ", l.contract_end, NULL" + DATED_LINES
            + "l.status = 'ACTIVE' AND l.contract_end < ? ORDER BY l.contract_end, l.id";
    private static final String PROMOTIONS_ENDING = LINE_SELECT + ", l.promotion_ends_on, pr.name" + DATED_LINES
            + "l.status = 'ACTIVE' AND l.promotion_ends_on >= ? AND l.promotion_ends_on <= ?"
            + " ORDER BY l.promotion_ends_on, l.id";

    private final JdbcTemplate jdbc;

    public InsightService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Insights insights(Instant now, Access access) {
        List<Alert> alerts = new ArrayList<>();
        Instant usageAsOf = null;
        if (access.usage()) {
            String latest = jdbc.queryForObject("SELECT MAX(recorded_at) FROM usage_records", String.class);
            if (latest != null) {
                usageAsOf = Instant.from(DB_TIME.parse(latest));
                alerts.addAll(roaming(usageAsOf, access));
            }
        }
        if (access.catalog()) {
            LocalDate today = LocalDate.ofInstant(now, DashboardOverviewService.SINGAPORE);
            alerts.addAll(retiredPlans(access));
            contractsEnding(today, access, alerts);
            outOfContract(today, access, alerts);
            promotionsEnding(today, access, alerts);
        }
        // List.sort is stable, so alerts of equal rank keep the order they were added
        // in.
        alerts.sort(Comparator.comparing(Alert::severity).thenComparing(Alert::affected, Comparator.reverseOrder()));
        return new Insights(now.toString(), usageAsOf == null ? null : usageAsOf.toString(), alerts);
    }

    private List<Alert> roaming(Instant asOf, Access access) {
        List<LineItem> items = jdbc.query(LINE_SELECT + """
                , SUM(u.cost), SUM(CASE WHEN u.type = 'DATA' THEN u.cost ELSE 0 END),
                  SUM(CASE WHEN u.type = 'CALL' THEN u.cost ELSE 0 END)
                FROM usage_records u JOIN mobile_lines l ON l.id = u.line_id
                """ + LINE_JOINS + """

                WHERE u.roaming = 1 AND u.recorded_at > ? AND u.recorded_at <= ? AND l.status = 'ACTIVE'
                GROUP BY l.id HAVING SUM(u.cost) >= ? ORDER BY SUM(u.cost) DESC, l.id
                """,
                (rs, i) -> item(rs, access, money(rs.getBigDecimal(6)), null,
                        "Data " + sgd(money(rs.getBigDecimal(7))) + ", calls " + sgd(money(rs.getBigDecimal(8)))),
                DashboardOverviewService.db(asOf.minus(ROAMING_WINDOW)), DashboardOverviewService.db(asOf),
                ROAMING_MEDIUM.doubleValue());
        List<Alert> alerts = new ArrayList<>();
        List<LineItem> medium = new ArrayList<>();
        for (LineItem item : items) {
            if (item.amount().compareTo(ROAMING_HIGH) >= 0) {
                alerts.add(new Alert("roaming-high-" + item.line_id(), Area.USAGE, Severity.HIGH, Check.ROAMING_CHARGES,
                        sgd(item.amount()) + " roaming charges on " + msisdn(item.msisdn()) + " in 7 days",
                        "Possible bill shock. Check whether the line has a roaming add-on, or contact the customer"
                                + " before the bill arrives.",
                        1, "Roaming, 7 days", ValueKind.AMOUNT, List.of(item)));
            } else {
                medium.add(item);
            }
        }
        if (!medium.isEmpty()) {
            alerts.add(new Alert("roaming-medium", Area.USAGE, Severity.MEDIUM, Check.ROAMING_CHARGES,
                    lines(medium.size()) + " with " + sgd(ROAMING_MEDIUM) + " to " + sgd(ROAMING_HIGH)
                            + " roaming charges in 7 days",
                    "Below bill-shock level, but these customers may save with a roaming add-on.", medium.size(),
                    "Roaming, 7 days", ValueKind.AMOUNT, medium));
        }
        return alerts;
    }

    private List<Alert> retiredPlans(Access access) {
        List<Object[]> plans = jdbc.query("""
                SELECT p.id, p.name, COUNT(*), SUM(p.monthly_price) FROM mobile_lines l JOIN plans p ON p.id = l.plan_id
                WHERE p.is_active = 0 AND l.status = 'ACTIVE' GROUP BY p.id, p.name ORDER BY COUNT(*) DESC, p.id
                """, (rs, i) -> new Object[] {rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4)});
        List<Alert> alerts = new ArrayList<>();
        for (Object[] plan : plans) {
            long count = (Long) plan[2];
            List<LineItem> items = jdbc.query(RETIRED_PLAN_LINES, datedItem(access), plan[0]);
            alerts.add(new Alert("retired-plan-" + plan[0], Area.PLANS, Severity.MEDIUM, Check.RETIRED_PLAN,
                    lines(count) + " on retired plan " + plan[1],
                    "The plan is no longer sold but still bills " + sgd(money(plan[3]))
                            + " a month. Offer these customers a current plan.",
                    count, "Line since", ValueKind.DATE, visible(items, access)));
        }
        return alerts;
    }

    private void contractsEnding(LocalDate today, Access access, List<Alert> alerts) {
        List<LineItem> items = jdbc.query(CONTRACTS_ENDING, datedItem(access), today.toString(),
                today.plusDays(ENDING_DAYS).toString());
        if (!items.isEmpty()) {
            alerts.add(new Alert("contracts-ending", Area.PLANS, Severity.MEDIUM, Check.CONTRACT_ENDING,
                    plural(items.size(), "contract ends", "contracts end") + " in the next " + ENDING_DAYS + " days",
                    "Customers coming out of contract are the most likely to switch operator. Offer a re-contract"
                            + " or handset upgrade before the end date.",
                    items.size(), "Contract ends", ValueKind.DATE, visible(items, access)));
        }
    }

    private void outOfContract(LocalDate today, Access access, List<Alert> alerts) {
        List<LineItem> items = jdbc.query(OUT_OF_CONTRACT, datedItem(access), today.toString());
        if (!items.isEmpty()) {
            alerts.add(new Alert("out-of-contract", Area.PLANS, Severity.LOW, Check.OUT_OF_CONTRACT,
                    plural(items.size(), "active line is", "active lines are") + " out of contract",
                    "These customers can leave at any time without an early-termination fee. Consider a retention"
                            + " or re-contract offer.",
                    items.size(), "Contract ended", ValueKind.DATE, visible(items, access)));
        }
    }

    private void promotionsEnding(LocalDate today, Access access, List<Alert> alerts) {
        List<LineItem> items = jdbc.query(PROMOTIONS_ENDING, datedItem(access), today.toString(),
                today.plusDays(ENDING_DAYS).toString());
        if (!items.isEmpty()) {
            alerts.add(new Alert("promotions-ending", Area.PLANS, Severity.MEDIUM, Check.PROMOTION_ENDING,
                    plural(items.size(), "promotional discount ends", "promotional discounts end") + " in the next "
                            + ENDING_DAYS + " days",
                    "These customers' bills go up when the discount ends. Let them know or offer a new deal before"
                            + " the price changes.",
                    items.size(), "Discount ends", ValueKind.DATE, visible(items, access)));
        }
    }

    private static RowMapper<LineItem> datedItem(Access access) {
        return (rs, i) -> item(rs, access, null, isoDate(rs.getString(6)), rs.getString(7));
    }

    private static LineItem item(ResultSet rs, Access access, BigDecimal amount, String date, String note)
            throws SQLException {
        boolean customers = access.customers();
        return new LineItem(rs.getLong(1), rs.getString(2), customers ? rs.getLong(3) : null,
                customers ? rs.getString(4) : null, rs.getString(5), amount, date, note);
    }

    /**
     * Roles without LINE_READ see how many lines an alert affects, but not which.
     */
    private static List<LineItem> visible(List<LineItem> items, Access access) {
        return access.lines() ? items : null;
    }

    /** Timestamps become ISO instants; plain dates stay "yyyy-mm-dd". */
    private static String isoDate(String value) {
        if (value == null || value.length() <= "yyyy-MM-dd".length()) {
            return value;
        }
        return Instant.from(DB_TIME.parse(value)).toString();
    }

    private static BigDecimal money(Object value) {
        BigDecimal amount = value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    static String sgd(BigDecimal amount) {
        return String.format(Locale.ROOT, "S$%,.2f", amount);
    }

    private static String msisdn(String number) {
        return number.length() == 8 ? number.substring(0, 4) + " " + number.substring(4) : number;
    }

    private static String lines(long count) {
        return plural(count, "active line", "active lines");
    }

    private static String plural(long count, String one, String many) {
        return count + " " + (count == 1 ? one : many);
    }

    /**
     * What the signed-in role may see; each check needs the permissions named here.
     */
    public record Access(boolean usage, boolean catalog, boolean lines, boolean customers) {

        /**
         * Roaming alerts name the line, so they need LINE_READ as well as USAGE_READ.
         */
        public static Access current() {
            boolean lines = Permission.LINE_READ.isGranted();
            return new Access(Permission.USAGE_READ.isGranted() && lines, Permission.CATALOG_READ.isGranted(), lines,
                    Permission.CUSTOMER_READ.isGranted());
        }
    }

    public enum Area {
        USAGE, PLANS
    }

    /** Declared most severe first; alerts are listed in this order. */
    public enum Severity {
        HIGH, MEDIUM, LOW
    }

    public enum Check {
        ROAMING_CHARGES, RETIRED_PLAN, CONTRACT_ENDING, OUT_OF_CONTRACT, PROMOTION_ENDING
    }

    /**
     * Whether {@link LineItem#amount} or {@link LineItem#date} holds an alert's
     * value.
     */
    public enum ValueKind {
        AMOUNT, DATE
    }

    /**
     * {@code usage_as_of} is the latest usage record, which the usage checks count
     * back from; null when the role cannot see usage or there is none.
     */
    public record Insights(String as_of, String usage_as_of, List<Alert> alerts) {
        public Insights {
            alerts = List.copyOf(alerts);
        }
    }

    /**
     * One finding. {@code lines} lists the affected lines, or is null when the role
     * may not see lines; {@code affected} is always the count.
     */
    public record Alert(String id, Area area, Severity severity, Check check, String title, String detail,
            long affected, String value_label, ValueKind value_kind, List<LineItem> lines) {
        @SuppressWarnings("PMD.NullAssignment")
        public Alert {
            lines = lines == null ? null : List.copyOf(lines);
        }
    }

    /**
     * An affected line. Customer fields are null for roles without CUSTOMER_READ.
     */
    public record LineItem(long line_id, String msisdn, Long customer_id, String customer_name, String plan_name,
            BigDecimal amount, String date, String note) {
    }
}
