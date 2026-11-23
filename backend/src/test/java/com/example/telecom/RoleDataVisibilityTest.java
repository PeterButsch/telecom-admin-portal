package com.example.telecom;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.security.StaffRole;
import org.junit.jupiter.api.Test;

/**
 * Roles without CUSTOMER_READ or LINE_READ get records with those details left
 * out.
 */
class RoleDataVisibilityTest extends IntegrationTestSupport {

    private static final String DEVICE = """
            {"imei":"%s","manufacturer":"Apple","model":"iPhone 17","msisdn":%s}
            """;

    private long createDevice(String imei, String msisdn) throws Exception {
        postJson("/api/devices", DEVICE.formatted(imei, msisdn == null ? "null" : "\"" + msisdn + "\""))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT id FROM devices WHERE imei = ?", Long.class, imei);
    }

    @Test
    void productManagersSeeDevicesWithoutLinesOrCustomers() throws Exception {
        createDevice("353918120000017", "91234567");
        mockMvc.perform(get("/api/devices").with(as(StaffRole.PRODUCT_MANAGER))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imei").value("353918120000017"))
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"))
                .andExpect(jsonPath("$[0].line_id").value(nullValue()))
                .andExpect(jsonPath("$[0].msisdn").value(nullValue()))
                .andExpect(jsonPath("$[0].customer_id").value(nullValue()))
                .andExpect(jsonPath("$[0].customer_name").value(nullValue()));
    }

    @Test
    void networkOpsSeeTheLineOfADeviceButNotTheCustomer() throws Exception {
        long id = createDevice("353918120000017", "91234567");
        mockMvc.perform(get("/api/devices/" + id).with(as(StaffRole.NETWORK_OPS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.msisdn").value("91234567"))
                .andExpect(jsonPath("$.customer_name").value(nullValue()));
        mockMvc.perform(get("/api/devices/" + id).with(as(StaffRole.CARE_AGENT))).andExpect(status().isOk())
                .andExpect(jsonPath("$.customer_name").value("Ada Tan"));
    }

    @Test
    void networkOpsSeeLinesWithoutCustomersAndCannotSearchByName() throws Exception {
        jdbc.update("UPDATE mobile_lines SET user_name = 'Ben Tan' WHERE id = 1");
        mockMvc.perform(get("/api/lines").param("q", "tan").with(as(StaffRole.NETWORK_OPS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/lines").param("q", "9123 4567").with(as(StaffRole.NETWORK_OPS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].account_number").value("1000000001"))
                .andExpect(jsonPath("$.content[0].customer_id").value(nullValue()))
                .andExpect(jsonPath("$.content[0].customer_name").value(nullValue()))
                .andExpect(jsonPath("$.content[0].user_name").value(nullValue()));
        mockMvc.perform(get("/api/lines/1").with(as(StaffRole.NETWORK_OPS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.line.customer_name").value(nullValue()));
        mockMvc.perform(get("/api/lines").param("q", "tan").with(as(StaffRole.CARE_AGENT))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].customer_name").value("Ada Tan"))
                .andExpect(jsonPath("$.content[0].user_name").value("Ben Tan"));
    }

    @Test
    void networkOpsSeeUsageAndCasesWithoutCustomers() throws Exception {
        jdbc.update("INSERT INTO usage_records (line_id, type, quantity, cost, roaming, recorded_at)"
                + " VALUES (1, 'DATA', 1, 0, 0, ?)", FIXTURE_TIME);
        postJson("/api/cases", """
                {"customerId":1,"msisdn":"91234567","category":"NETWORK","channel":"PHONE",
                 "subject":"No signal at home","priority":"LOW"}
                """).andExpect(status().isCreated());
        mockMvc.perform(get("/api/usage").with(as(StaffRole.NETWORK_OPS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].msisdn").value("91234567"))
                .andExpect(jsonPath("$.content[0].customer_id").value(nullValue()))
                .andExpect(jsonPath("$.content[0].customer_name").value(nullValue()));
        mockMvc.perform(get("/api/cases").with(as(StaffRole.NETWORK_OPS))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].msisdn").value("91234567"))
                .andExpect(jsonPath("$.content[0].customer_id").value(nullValue()))
                .andExpect(jsonPath("$.content[0].customer_number").value(nullValue()))
                .andExpect(jsonPath("$.content[0].customer_name").value(nullValue()));
        mockMvc.perform(get("/api/cases").with(as(StaffRole.FRAUD_ANALYST))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customer_name").value("Ada Tan"));
    }

    @Test
    void networkOpsCannotFilterByCustomer() throws Exception {
        jdbc.update("INSERT INTO usage_records (line_id, type, quantity, cost, roaming, recorded_at)"
                + " VALUES (1, 'DATA', 1, 0, 0, ?), (3, 'DATA', 1, 0, 0, ?)", FIXTURE_TIME, FIXTURE_TIME);
        postJson("/api/cases", """
                {"customerId":1,"category":"GENERAL","channel":"PHONE","subject":"Address change","priority":"LOW"}
                """).andExpect(status().isCreated());
        int allLines = jdbc.queryForObject("SELECT COUNT(*) FROM mobile_lines", Integer.class);
        int adaLines = jdbc.queryForObject(
                "SELECT COUNT(*) FROM mobile_lines l JOIN accounts a ON a.id = l.account_id WHERE a.customer_id = 1",
                Integer.class);

        mockMvc.perform(get("/api/lines").param("customerId", "1").with(as(StaffRole.CARE_AGENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(adaLines));
        mockMvc.perform(get("/api/lines").param("customerId", "1").with(as(StaffRole.NETWORK_OPS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(allLines));
        mockMvc.perform(get("/api/usage").param("customerId", "1").with(as(StaffRole.CARE_AGENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/usage").param("customerId", "1").with(as(StaffRole.NETWORK_OPS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/cases").param("customerId", "2").with(as(StaffRole.CARE_AGENT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/cases").param("customerId", "2").with(as(StaffRole.NETWORK_OPS)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void onlyAdministratorsDeleteDevices() throws Exception {
        long id = createDevice("353918120000017", null);
        mockMvc.perform(delete("/api/devices/" + id).with(as(StaffRole.CARE_AGENT))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/devices/" + id).with(as(StaffRole.CARE_TEAM_LEAD)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/devices/" + id).with(as(StaffRole.ADMIN))).andExpect(status().isNoContent());
    }
}
