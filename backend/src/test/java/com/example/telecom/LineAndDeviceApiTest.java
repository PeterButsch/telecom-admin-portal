package com.example.telecom;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class LineAndDeviceApiTest extends IntegrationTestSupport {

    private static final String DEVICE = """
            {"imei":"%s","manufacturer":"Apple","model":"iPhone 17","msisdn":%s}
            """;

    private long createDevice(String imei, String msisdn) throws Exception {
        postJson("/api/devices", DEVICE.formatted(imei, msisdn == null ? "null" : "\"" + msisdn + "\""))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT id FROM devices WHERE imei = ?", Long.class, imei);
    }

    @Test
    void listsLinesByCustomerAndStatus() throws Exception {
        mockMvc.perform(get("/api/lines").param("customerId", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].msisdn").value("91234567"))
                .andExpect(jsonPath("$.content[0].plan_name").value("Test 5G 100GB"))
                .andExpect(jsonPath("$.content[0].customer_name").value("Ada Tan"));
        mockMvc.perform(get("/api/lines").param("status", "TERMINATED")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(2));
        mockMvc.perform(get("/api/lines").param("q", "9876 5432")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customer_type").value("BUSINESS"));
    }

    @Test
    void lineDetailIncludesTheAssignedDevice() throws Exception {
        createDevice("353918120000017", "+65 9123 4567");
        mockMvc.perform(get("/api/lines/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.line.msisdn").value("91234567"))
                .andExpect(jsonPath("$.device.imei").value("353918120000017"))
                .andExpect(jsonPath("$.add_ons.length()").value(0));
        mockMvc.perform(get("/api/lines/404")).andExpect(status().isNotFound());
    }

    @Test
    void deviceIsAssignedByMobileNumber() throws Exception {
        long id = createDevice("353918120000017", "9123 4567");
        mockMvc.perform(get("/api/devices/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED")).andExpect(jsonPath("$.msisdn").value("91234567"))
                .andExpect(jsonPath("$.customer_name").value("Ada Tan"));
    }

    @Test
    void aLineHoldsOneDeviceAndTerminatedLinesNone() throws Exception {
        createDevice("353918120000017", "91234567");
        postJson("/api/devices", DEVICE.formatted("353918120000025", "\"91234567\"")).andExpect(status().isConflict());
        postJson("/api/devices", DEVICE.formatted("353918120000025", "\"81234567\""))
                .andExpect(status().isBadRequest());
        postJson("/api/devices", DEVICE.formatted("353918120000025", "\"99999999\""))
                .andExpect(status().isBadRequest());
        postJson("/api/devices", DEVICE.formatted("353918120000025", "\"61234567\"")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.msisdn").exists());
    }

    @Test
    void releasingADeviceClearsTheLine() throws Exception {
        long id = createDevice("353918120000017", "91234567");
        mockMvc.perform(delete("/api/devices/" + id)).andExpect(status().isConflict());
        patchJson("/api/devices/" + id, "{\"status\":\"AVAILABLE\",\"msisdn\":\"91234567\"}")
                .andExpect(status().isBadRequest());
        patchJson("/api/devices/" + id, "{\"status\":\"AVAILABLE\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.msisdn").value(nullValue())).andExpect(jsonPath("$.model").value("iPhone 17"));
        patchJson("/api/devices/" + id, "{\"status\":\"ASSIGNED\"}").andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/devices/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void duplicateImeiIsAConflict() throws Exception {
        createDevice("353918120000017", null);
        postJson("/api/devices", DEVICE.formatted("353918120000017", "null")).andExpect(status().isConflict());
        postJson("/api/devices", DEVICE.formatted("12AB", "null")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.imei").exists());
    }
}
