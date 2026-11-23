package com.example.telecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.security.StaffRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

class AuditTrailTest extends IntegrationTestSupport {

    private static final String NEW_CUSTOMER = """
            {"type":"INDIVIDUAL","firstName":"Mei","lastName":"Lim","idType":"NRIC","idNumber":"S7654321F",
             "dateOfBirth":"1985-03-02","email":"mei.lim@example.com","phone":"+65 9876 5432"}""";

    @Test
    void customerChangesRecordWhoDidWhatWithoutPersonalValues() throws Exception {
        postJson("/api/customers", NEW_CUSTOMER).andExpect(status().isCreated());
        patchJson("/api/customers/1", "{\"email\":\"ada.new@example.com\",\"status\":\"SUSPENDED\"}")
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customers/1")).andExpect(status().isOk());

        List<Map<String, Object>> events = auditEvents();
        assertEquals(List.of("CUSTOMER_CREATED", "CUSTOMER_UPDATED", "CUSTOMER_VIEWED"),
                events.stream().map(e -> e.get("action")).toList());
        Map<String, Object> created = events.get(0);
        assertEquals("admin", created.get("actor_username"));
        assertEquals("ADMIN", created.get("actor_role"));
        assertEquals("CUSTOMER", created.get("entity_type"));
        assertEquals("SUCCESS", created.get("outcome"));
        assertEquals("Updated customer C1000001: email, status ACTIVE → SUSPENDED", events.get(1).get("summary"));
        for (Map<String, Object> event : events) {
            String summary = (String) event.get("summary");
            assertFalse(summary.contains("S7654321F") || summary.contains(ADA_NRIC), summary);
            assertFalse(summary.contains("ada.new@example.com"), summary);
        }
    }

    @Test
    void deniedRequestPathsAreRecordedWithoutIdNumbers() throws Exception {
        mockMvc.perform(get("/api/lookup/" + ADA_NRIC)).andExpect(status().isForbidden());
        Map<String, Object> event = auditEvents().getLast();
        assertEquals("ACCESS_DENIED", event.get("action"));
        String entityId = (String) event.get("entity_id");
        assertTrue(entityId.startsWith("GET /api/lookup/"), entityId);
        assertFalse(entityId.contains(ADA_NRIC), entityId);
        assertFalse(((String) event.get("summary")).contains(ADA_NRIC), (String) event.get("summary"));
    }

    @Test
    void planPriceChangesShowOldAndNewValues() throws Exception {
        putJson("/api/plans/1", """
                {"code":"TEST-5G","name":"Test 5G 100GB","brand":"SINGTEL","category":"POSTPAID",
                 "segment":"CONSUMER","monthlyPrice":45.00,"dataLimitGb":100,"contractMonths":24,"network":"5G"}""")
                .andExpect(status().isOk());
        Map<String, Object> event = auditEvents().getLast();
        assertEquals("PLAN_UPDATED", event.get("action"));
        assertEquals("1", event.get("entity_id"));
        assertTrue(((String) event.get("summary")).contains("monthly price 40 → 45"), (String) event.get("summary"));
    }

    @Test
    void unchangedUpdatesAreNotAudited() throws Exception {
        patchJson("/api/customers/1", "{\"phone\":\"+65 9123 4567\"}").andExpect(status().isOk());
        assertTrue(auditEvents().isEmpty());
    }

    @Test
    void failedChangesLeaveNoAuditEntry() throws Exception {
        postJson("/api/customers", NEW_CUSTOMER.replace("mei.lim@example.com", "ada.tan@example.com"))
                .andExpect(status().isConflict());
        assertTrue(auditEvents().isEmpty());
    }

    @Test
    void theTrailCannotBeChangedOrDeleted() throws Exception {
        patchJson("/api/customers/1", "{\"status\":\"SUSPENDED\"}").andExpect(status().isOk());
        Object id = auditEvents().getLast().get("id");
        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE audit_events SET summary = 'nothing happened' WHERE id = ?", id));
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM audit_events WHERE id = ?", id));
    }

    @Test
    void auditorsCanFilterTheTrail() throws Exception {
        patchJson("/api/customers/1", "{\"status\":\"SUSPENDED\"}").andExpect(status().isOk());
        patchJson("/api/customers/2", "{\"status\":\"SUSPENDED\"}").andExpect(status().isOk());
        mockMvc.perform(get("/api/audit-events")
                .with(as(StaffRole.FRAUD_ANALYST)).param("entityType", "CUSTOMER").param("entityId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(
                        jsonPath("$.content[0].summary").value("Updated customer C1000002: status ACTIVE → SUSPENDED"))
                .andExpect(jsonPath("$.content[0].actor_role_label").value("Administrator"));
        mockMvc.perform(get("/api/audit-events").param("action", "CUSTOMER_UPDATED").param("q", "c1000001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].entity_id").value("1"));
        mockMvc.perform(get("/api/audit-events").param("action", "HACKED")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/audit-events/filters")).andExpect(status().isOk())
                .andExpect(jsonPath("$.actions[0]").value("LOGIN"));
    }

    @Test
    void auditDateRangeCoversWholeSingaporeDays() throws Exception {
        String insert = "INSERT INTO audit_events (occurred_at, actor_username, action, summary, outcome)"
                + " VALUES (?, 'tester', 'LOGIN', ?, 'SUCCESS')";
        jdbc.update(insert, "2026-01-31 15:59:59.999", "day-test before");
        jdbc.update(insert, "2026-01-31 16:00:00.000", "day-test first");
        jdbc.update(insert, "2026-02-01 15:59:59.999", "day-test last");
        jdbc.update(insert, "2026-02-01 16:00:00.000", "day-test after");
        mockMvc.perform(get("/api/audit-events").param("q", "day-test").param("dateFrom", "2026-02-01").param("dateTo",
                "2026-02-01")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].summary").value("day-test last"))
                .andExpect(jsonPath("$.content[1].summary").value("day-test first"));
    }

    @Test
    void auditSearchTreatsWildcardsLiterally() throws Exception {
        String insert = "INSERT INTO audit_events (occurred_at, actor_username, action, summary, outcome)"
                + " VALUES ('2026-02-01 00:00:00.000', 'tester', 'LOGIN', ?, 'SUCCESS')";
        jdbc.update(insert, "Discount 50% applied");
        jdbc.update(insert, "Discount 50 applied");
        mockMvc.perform(get("/api/audit-events").param("q", "50%")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].summary").value("Discount 50% applied"));
    }
}
