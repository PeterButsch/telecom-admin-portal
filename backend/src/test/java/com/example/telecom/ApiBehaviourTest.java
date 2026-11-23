package com.example.telecom;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

class ApiBehaviourTest extends IntegrationTestSupport {

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        postJson("/api/customers", "{not json").andExpect(status().isBadRequest());
    }

    @Test
    void invalidEnumQueryParameterNamesTheAllowedValues() throws Exception {
        mockMvc.perform(get("/api/cases").param("priority", "CRITICAL")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Invalid value 'CRITICAL' for priority. Allowed values: LOW, MEDIUM, HIGH, URGENT."));
        mockMvc.perform(get("/api/usage").param("type", "VIDEO")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidDatesAreBadRequests() throws Exception {
        mockMvc.perform(get("/api/usage").param("dateFrom", "2026-13-40")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/usage").param("dateFrom", "2026-02-02").param("dateTo", "2026-02-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        mockMvc.perform(get("/api/lines").param("size", "5000").param("page", "-3")).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void dashboardRevenueOnlyCountsActiveLines() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")).andExpect(status().isOk())
                .andExpect(jsonPath("$.active_lines").value(2)).andExpect(jsonPath("$.monthly_revenue").value(80.00))
                .andExpect(jsonPath("$.active_customers").value(2)).andExpect(jsonPath("$.open_cases").value(0));
        mockMvc.perform(get("/api/dashboard/revenue-by-plan")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test 5G 100GB"))
                .andExpect(jsonPath("$[0].revenue").value(80.00));
        mockMvc.perform(get("/api/dashboard/lines-by-plan")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value(2));
    }

    @Test
    void databaseRejectsOrphansAndInvalidNumbers() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("INSERT INTO usage_records (line_id, type, quantity, cost, recorded_at)"
                        + " VALUES (999999, 'DATA', 1, 0, '2026-02-01 00:00:00.000')"));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE mobile_lines SET msisdn = '61234567'" + " WHERE id = 1"));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE mobile_lines SET status = 'ACTIVE'" + " WHERE id = 2"));
    }

    @Test
    void healthReportsTheDatabase() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.database").value("up"));
    }

    @Test
    void oversizedRequestBodyIsRejected() throws Exception {
        postJson("/api/customers", "{\"firstName\":\"" + "x".repeat(5000) + "\"}")
                .andExpect(status().isPayloadTooLarge());
    }
}
