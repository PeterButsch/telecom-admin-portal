package com.example.telecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.security.StaffRole;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AuthorizationTest extends IntegrationTestSupport {

    private static final String CASE_BODY = "{\"customerId\":1,\"category\":\"BILLING\",\"channel\":\"PHONE\","
            + "\"subject\":\"Bill query\",\"priority\":\"LOW\"}";

    static Stream<Arguments> accessRules() {
        return Stream.of(Arguments.of(StaffRole.PRODUCT_MANAGER, "GET", "/api/customers", false),
                Arguments.of(StaffRole.PRODUCT_MANAGER, "GET", "/api/lines", false),
                Arguments.of(StaffRole.PRODUCT_MANAGER, "GET", "/api/plans", true),
                Arguments.of(StaffRole.PRODUCT_MANAGER, "GET", "/api/dashboard/stats", true),
                Arguments.of(StaffRole.NETWORK_OPS, "GET", "/api/lines", true),
                Arguments.of(StaffRole.NETWORK_OPS, "GET", "/api/customers", false),
                Arguments.of(StaffRole.NETWORK_OPS, "POST", "/api/cases", false),
                Arguments.of(StaffRole.NETWORK_OPS, "PUT", "/api/plans/1", false),
                Arguments.of(StaffRole.CARE_AGENT, "POST", "/api/cases", true),
                Arguments.of(StaffRole.CARE_AGENT, "DELETE", "/api/customers/2", false),
                Arguments.of(StaffRole.CARE_AGENT, "PUT", "/api/plans/1", false),
                Arguments.of(StaffRole.CARE_AGENT, "GET", "/api/audit-events", false),
                Arguments.of(StaffRole.CARE_TEAM_LEAD, "DELETE", "/api/customers/2", true),
                Arguments.of(StaffRole.CARE_TEAM_LEAD, "GET", "/api/audit-events", true),
                Arguments.of(StaffRole.CARE_TEAM_LEAD, "GET", "/api/staff-users", false),
                Arguments.of(StaffRole.FRAUD_ANALYST, "GET", "/api/audit-events", true),
                Arguments.of(StaffRole.FRAUD_ANALYST, "PATCH", "/api/customers/1", false),
                Arguments.of(StaffRole.FRAUD_ANALYST, "POST", "/api/cases", true),
                Arguments.of(StaffRole.ADMIN, "GET", "/api/staff-users", true),
                Arguments.of(StaffRole.ADMIN, "GET", "/api/not-a-real-endpoint", false));
    }

    @ParameterizedTest(name = "{0} {1} {2} allowed={3}")
    @MethodSource("accessRules")
    void eachRoleOnlyReachesItsEndpoints(StaffRole role, String method, String url, boolean allowed) throws Exception {
        int status = mockMvc.perform(request(method, url).with(as(role))).andReturn().getResponse().getStatus();
        if (allowed) {
            assertTrue(status != 401 && status != 403, method + " " + url + " returned " + status);
        } else {
            assertEquals(403, status, method + " " + url);
        }
    }

    @Test
    void deniedRequestsAreAudited() throws Exception {
        mockMvc.perform(get("/api/audit-events").with(as(StaffRole.CARE_AGENT))).andExpect(status().isForbidden());
        Map<String, Object> event = auditEvents().getLast();
        assertEquals("ACCESS_DENIED", event.get("action"));
        assertEquals("DENIED", event.get("outcome"));
        assertEquals("care_agent", event.get("actor_username"));
        assertEquals("GET /api/audit-events", event.get("entity_id"));
    }

    @Test
    void aRoleChangeAppliesToTheNextRequest() throws Exception {
        MockHttpSession session = signIn(StaffRole.CARE_AGENT);
        anonymousMvc.perform(get("/api/audit-events").session(session)).andExpect(status().isForbidden());
        patchJson("/api/staff-users/" + staffId(StaffRole.CARE_AGENT), "{\"role\":\"FRAUD_ANALYST\"}")
                .andExpect(status().isOk());
        anonymousMvc.perform(get("/api/audit-events").session(session)).andExpect(status().isOk());
        anonymousMvc.perform(patch("/api/customers/1").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"+65 9000 0000\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivationEndsExistingSessions() throws Exception {
        MockHttpSession session = signIn(StaffRole.CARE_AGENT);
        patchJson("/api/staff-users/" + staffId(StaffRole.CARE_AGENT), "{\"active\":false}").andExpect(status().isOk());
        anonymousMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void aPasswordResetEndsExistingSessions() throws Exception {
        MockHttpSession session = signIn(StaffRole.CARE_AGENT);
        postJson("/api/staff-users/" + staffId(StaffRole.CARE_AGENT) + "/password",
                "{\"password\":\"Reset-Password-2026\"}").andExpect(status().isOk());
        anonymousMvc.perform(get("/api/customers").session(session)).andExpect(status().isUnauthorized());
        login("care_agent", "Reset-Password-2026").andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder request(String method, String url) {
        return switch (method) {
            case "POST" -> post(url).contentType(MediaType.APPLICATION_JSON).content(CASE_BODY);
            case "PATCH" -> patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"+65 9000 0000\"}");
            case "PUT" -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                    .contentType(MediaType.APPLICATION_JSON).content("{}");
            case "DELETE" -> delete(url);
            default -> get(url);
        };
    }
}
