package com.example.telecom.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.telecom.model.IdentityNumbers;
import com.example.telecom.security.StaffRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {"app.demo-data.enabled=true", "app.demo-data.staff-password=Seed-Test-Pass-1",
        "spring.datasource.url=jdbc:sqlite:file:telecom-seed-test?mode=memory&cache=shared&foreign_keys=true"
                + "&date_class=TEXT"})
class DemoDataSeederTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DemoDataSeeder seeder;

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    @Test
    void seedsTheExpectedVolumes() {
        assertEquals(CustomerSeeder.INDIVIDUALS + CustomerSeeder.BUSINESSES, count("SELECT COUNT(*) FROM customers"));
        assertEquals(15, count("SELECT COUNT(*) FROM plans"));
        assertTrue(count("SELECT COUNT(*) FROM mobile_lines") > 1500);
        assertTrue(count("SELECT COUNT(*) FROM usage_records") > 100_000);
        assertEquals(ActivitySeeder.CASES, count("SELECT COUNT(*) FROM support_cases"));
    }

    @Test
    void identityNumbersAreValidForTheirType() {
        List<Map<String, Object>> rows = jdbc
                .queryForList("SELECT id_type, id_number FROM customers WHERE id_type IN ('NRIC', 'FIN')");
        assertTrue(rows.size() > 400);
        for (Map<String, Object> row : rows) {
            String id = (String) row.get("id_number");
            assertTrue(IdentityNumbers.isValidNricOrFin(id), id);
            assertEquals("NRIC".equals(row.get("id_type")), IdentityNumbers.isNricPrefix(id), id);
        }
        assertEquals(0, count("SELECT COUNT(*) FROM customers WHERE type = 'BUSINESS' AND id_number IS NOT NULL"));
    }

    @Test
    void someIndividualsExceedTheImdaLineLimits() {
        String perPerson = "SELECT COUNT(*) FROM (SELECT a.customer_id FROM mobile_lines l"
                + " JOIN accounts a ON a.id = l.account_id JOIN customers c ON c.id = a.customer_id"
                + " WHERE c.type = 'INDIVIDUAL' AND l.status <> 'TERMINATED' AND a.type = ?"
                + " GROUP BY a.customer_id HAVING COUNT(*) > ?)";
        assertTrue(jdbc.queryForObject(perPerson, Long.class, "POSTPAID", 10) > 0);
        assertTrue(jdbc.queryForObject(perPerson, Long.class, "PREPAID", 3) > 0);
    }

    @Test
    void lineStatusesMatchTheirCustomersAndDevices() {
        assertEquals(0, count("SELECT COUNT(*) FROM mobile_lines l JOIN accounts a ON a.id = l.account_id"
                + " JOIN customers c ON c.id = a.customer_id WHERE c.status = 'CLOSED' AND l.status <> 'TERMINATED'"));
        assertEquals(0, count("SELECT COUNT(*) FROM devices d JOIN mobile_lines l ON l.id = d.line_id"
                + " WHERE l.status = 'TERMINATED'"));
        assertEquals(0, count("SELECT COUNT(*) FROM usage_records u JOIN mobile_lines l ON l.id = u.line_id"
                + " WHERE u.recorded_at < l.activated_at"));
    }

    @Test
    void theWorkQueueHasOpenCasesWithSomeSlaBreaches() {
        long open = count("SELECT COUNT(*) FROM support_cases WHERE status NOT IN ('RESOLVED', 'CLOSED')");
        assertTrue(open >= 20, "open cases: " + open);
        assertTrue(count("SELECT COUNT(*) FROM support_cases WHERE resolved_at > sla_due_at") > 0);
    }

    @Test
    void residentsAreAdultsWhenTheyJoin() {
        assertEquals(0, count("SELECT COUNT(*) FROM customers WHERE type = 'INDIVIDUAL'"
                + " AND date_of_birth > date(created_at, '-18 years')"));
    }

    @Test
    void onlyHandsetPlansComeWithAHandset() {
        assertEquals(0, count("SELECT COUNT(*) FROM devices d JOIN mobile_lines l ON l.id = d.line_id"
                + " JOIN plans p ON p.id = l.plan_id WHERE p.category <> 'POSTPAID'"));
        assertTrue(count("SELECT COUNT(*) FROM devices WHERE line_id IS NOT NULL") > 300);
    }

    @Test
    void promotionsRunForTheirOwnDuration() {
        String active = "SELECT COUNT(*) FROM mobile_lines l JOIN promotions p ON p.id = l.promotion_id"
                + " WHERE l.activated_at IS NOT NULL";
        assertTrue(count(active) > 200);
        // A month is 28 to 31 days, so allow a few days either way over the whole
        // duration.
        assertEquals(0, count(active + " AND ABS(julianday(l.promotion_ends_on) - julianday(date(l.activated_at))"
                + " - p.duration_months * 30.436875) > 4"));
        assertEquals(3, count("SELECT COUNT(DISTINCT p.duration_months) FROM mobile_lines l"
                + " JOIN promotions p ON p.id = l.promotion_id"));
    }

    @Test
    void casesFallWithinTheCustomerAndLineLifetimes() {
        assertEquals(0, count("SELECT COUNT(*) FROM support_cases s JOIN customers c ON c.id = s.customer_id"
                + " WHERE s.created_at < c.created_at"));
        assertEquals(0,
                count("SELECT COUNT(*) FROM support_cases s JOIN mobile_lines l ON l.id = s.line_id"
                        + " WHERE l.activated_at IS NULL OR s.created_at < l.activated_at"
                        + " OR (l.terminated_at IS NOT NULL AND s.created_at > l.terminated_at)"));
    }

    @Test
    void businessesHaveLineCountsAcrossEachSizeBand() {
        String perBusiness = "SELECT COUNT(*) FROM (SELECT a.customer_id, COUNT(*) AS n FROM mobile_lines l"
                + " JOIN accounts a ON a.id = l.account_id JOIN customers c ON c.id = a.customer_id"
                + " WHERE c.type = 'BUSINESS' GROUP BY a.customer_id) WHERE ";
        assertEquals(CustomerSeeder.BUSINESSES, count(perBusiness + "n BETWEEN 5 AND 80"));
        // The top of the small and medium bands.
        assertTrue(count(perBusiness + "n BETWEEN 11 AND 15") > 0);
        assertTrue(count(perBusiness + "n BETWEEN 33 AND 40") > 0);
    }

    @Test
    void createsOneDemoStaffAccountPerRole() {
        assertEquals(StaffRole.values().length, count("SELECT COUNT(DISTINCT role) FROM staff_users"));
        assertEquals(0, count("SELECT COUNT(*) FROM staff_users WHERE password_hash NOT LIKE '$2%'"));
    }

    @Test
    void doesNotSeedTwice() {
        long customers = count("SELECT COUNT(*) FROM customers");
        seeder.afterSingletonsInstantiated();
        assertEquals(customers, count("SELECT COUNT(*) FROM customers"));
    }
}
