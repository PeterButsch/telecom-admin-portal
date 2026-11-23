package com.example.telecom;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsageAndCaseApiTest extends IntegrationTestSupport {

    private static final String CASE = """
            {"customerId":%d,"msisdn":%s,"category":"NETWORK","channel":"PHONE","subject":"No signal at home",
             "priority":"%s"}
            """;

    private void usage(long lineId, String type, String recordedAt) {
        jdbc.update("INSERT INTO usage_records (line_id, type, quantity, cost, roaming, recorded_at)"
                + " VALUES (?, ?, 1, 0, 0, ?)", lineId, type, recordedAt);
    }

    @Test
    void usageDateRangeCoversWholeSingaporeDays() throws Exception {
        // 1 February in Singapore runs from 31 January 16:00 to 1 February 16:00 UTC.
        usage(1, "DATA", "2026-01-31 15:59:59.999");
        usage(1, "DATA", "2026-01-31 16:00:00.000");
        usage(1, "SMS", "2026-02-01 15:59:59.999");
        usage(1, "DATA", "2026-02-01 16:00:00.000");
        mockMvc.perform(get("/api/usage").param("dateFrom", "2026-02-01").param("dateTo", "2026-02-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].recorded_at").value("2026-02-01T15:59:59.999Z"))
                .andExpect(jsonPath("$.content[0].unit").value("SMS"))
                .andExpect(jsonPath("$.content[1].recorded_at").value("2026-01-31T16:00:00Z"));
    }

    @Test
    void usageFiltersByMobileNumberAndCustomer() throws Exception {
        usage(1, "CALL", "2026-02-01 10:00:00.000");
        usage(3, "CALL", "2026-02-01 11:00:00.000");
        mockMvc.perform(get("/api/usage").param("msisdn", "+65 9123 4567")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].line_id").value(1))
                .andExpect(jsonPath("$.content[0].unit").value("MIN"));
        mockMvc.perform(get("/api/usage").param("customerId", "2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customer_name").value("Lion City Logistics Pte. Ltd."));
        mockMvc.perform(get("/api/usage").param("msisdn", "1234")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.msisdn").exists());
    }

    @Test
    void newCaseGetsANumberAndAnSlaFromItsPriority() throws Exception {
        String body = postJson("/api/cases", CASE.formatted(1, "\"9123 4567\"", "URGENT"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.case_number").value("CS1000001")).andExpect(jsonPath("$.line_id").value(1))
                .andExpect(jsonPath("$.sla_breached").value(false)).andReturn().getResponse().getContentAsString();
        Instant created = Instant.parse(JsonPath.read(body, "$.created_at"));
        Instant due = Instant.parse(JsonPath.read(body, "$.sla_due_at"));
        assertEquals(Duration.ofHours(4), Duration.between(created, due));
        assertEquals(created, Instant.parse(
                jdbc.queryForObject("SELECT replace(created_at, ' ', 'T') || 'Z' FROM support_cases", String.class)));
    }

    @Test
    void caseLineMustBelongToTheCustomer() throws Exception {
        postJson("/api/cases", CASE.formatted(2, "\"91234567\"", "LOW")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.msisdn").exists());
        postJson("/api/cases", CASE.formatted(404, "null", "LOW")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerId").exists());
        postJson("/api/cases", CASE.formatted(2, "null", "LOW")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.line_id").value(nullValue()));
    }

    @Test
    void resolvingAndReopeningTracksTheResolutionTime() throws Exception {
        postJson("/api/cases", CASE.formatted(1, "null", "HIGH")).andExpect(status().isCreated());
        String url = "/api/cases/" + jdbc.queryForObject("SELECT id FROM support_cases", Long.class);
        patchJson(url, "{\"status\":\"RESOLVED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved_at").isNotEmpty());
        patchJson(url, "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved_at").value(nullValue()))
                .andExpect(jsonPath("$.subject").value("No signal at home"));
        patchJson(url, "{\"status\":\"CLOSED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved_at").isNotEmpty());
        patchJson(url, "{\"status\":\"OPEN\"}").andExpect(status().isConflict());
        mockMvc.perform(delete(url)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void changingPriorityRecalculatesTheSlaFromTheOpeningTime() throws Exception {
        jdbc.update("INSERT INTO support_cases (id, case_number, customer_id, category, channel, subject, priority,"
                + " status, sla_due_at, created_at) VALUES (7, 'CS1000007', 1, 'BILLING', 'CHAT', 'Bill query', 'LOW',"
                + " 'OPEN', '2026-02-06 09:00:00.000', '2026-02-01 09:00:00.000')");
        patchJson("/api/cases/7", "{\"priority\":\"HIGH\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.sla_due_at").value("2026-02-02T09:00:00Z"))
                .andExpect(jsonPath("$.sla_breached").value(true));
    }

    @Test
    void casesCanBeSearchedAndFiltered() throws Exception {
        postJson("/api/cases", CASE.formatted(1, "null", "LOW")).andExpect(status().isCreated());
        postJson("/api/cases", CASE.formatted(2, "null", "URGENT")).andExpect(status().isCreated());
        mockMvc.perform(get("/api/cases").param("priority", "URGENT")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customer_number").value("C1000002"));
        mockMvc.perform(get("/api/cases").param("q", "cs1000001")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/cases").param("customerId", "1").param("status", "OPEN")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
