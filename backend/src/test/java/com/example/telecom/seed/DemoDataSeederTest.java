package com.example.telecom.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.telecom.model.IdentityNumbers;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {"app.demo-data.enabled=true",
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
    void doesNotSeedTwice() {
        long customers = count("SELECT COUNT(*) FROM customers");
        seeder.afterSingletonsInstantiated();
        assertEquals(customers, count("SELECT COUNT(*) FROM customers"));
    }
}
