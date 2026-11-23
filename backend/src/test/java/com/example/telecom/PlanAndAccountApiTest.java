package com.example.telecom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class PlanAndAccountApiTest extends IntegrationTestSupport {

    private static final String PLAN = """
            {"code":"%s","name":"Test SIM Only","brand":"GOMO","category":"%s","segment":"CONSUMER",
             "monthlyPrice":20.00,"dataLimitGb":200,"contractMonths":%d,"network":"5G"}
            """;

    @Test
    void plansReportLiveLineCounts() throws Exception {
        mockMvc.perform(get("/api/plans")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.code == 'TEST-5G')].line_count").value(2))
                .andExpect(jsonPath("$[?(@.code == 'TEST-PREPAID')].line_count").value(0));
        mockMvc.perform(get("/api/plans/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.monthly_price").value(40.00)).andExpect(jsonPath("$.contract_months").value(24));
    }

    @Test
    void createsAPlanWithAnUpperCaseCode() throws Exception {
        postJson("/api/plans", PLAN.formatted("gomo-test", "SIM_ONLY", 0)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GOMO-TEST")).andExpect(jsonPath("$.is_active").value(true));
        postJson("/api/plans", PLAN.formatted("GOMO-TEST", "SIM_ONLY", 0)).andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidContractTerms() throws Exception {
        postJson("/api/plans", PLAN.formatted("X-1", "SIM_ONLY", 18)).andExpect(status().isBadRequest());
        postJson("/api/plans", PLAN.formatted("X-2", "PREPAID", 12)).andExpect(status().isBadRequest());
        postJson("/api/plans", """
                {"code":"X-3","name":"Bad","brand":"GOMO","category":"SIM_ONLY","segment":"CONSUMER","monthlyPrice":-1}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.monthlyPrice").exists());
    }

    @Test
    void planCodeCannotChangeAndOmittedActiveFlagIsKept() throws Exception {
        jdbc.update("UPDATE plans SET is_active = 0 WHERE id = 2");
        putJson("/api/plans/2", PLAN.formatted("OTHER", "PREPAID", 0)).andExpect(status().isBadRequest());
        putJson("/api/plans/2", PLAN.formatted("TEST-PREPAID", "PREPAID", 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test SIM Only")).andExpect(jsonPath("$.is_active").value(false));
    }

    @Test
    void planInUseCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/plans/1")).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/plans/2")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/plans/2")).andExpect(status().isNotFound());
    }

    @Test
    void activePlansExcludeRetiredOnes() throws Exception {
        jdbc.update("UPDATE plans SET is_active = 0 WHERE id = 2");
        mockMvc.perform(get("/api/plans/active")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("TEST-5G"));
    }

    @Test
    void accountsListTheirLines() throws Exception {
        mockMvc.perform(get("/api/accounts").param("customerId", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].line_count").value(2))
                .andExpect(jsonPath("$[0].payment_method").value("GIRO"));
    }

    @Test
    void prepaidAccountsArePaidByTopUp() throws Exception {
        postJson("/api/accounts", "{\"customerId\":1,\"type\":\"PREPAID\",\"paymentMethod\":\"GIRO\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/accounts", "{\"customerId\":1,\"type\":\"PREPAID\",\"paymentMethod\":\"TOP_UP\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.bill_cycle_day").doesNotExist())
                .andExpect(jsonPath("$.account_number").isNotEmpty());
        postJson("/api/accounts", "{\"customerId\":404,\"type\":\"POSTPAID\",\"paymentMethod\":\"GIRO\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void addOnsAndPromotionsAreListed() throws Exception {
        jdbc.update(
                "INSERT INTO add_ons (code, name, category, monthly_price) VALUES ('ROAM-1', 'Roam', 'ROAMING', 15)");
        jdbc.update("INSERT INTO promotions (code, name, discount_type, discount_value, duration_months, valid_from)"
                + " VALUES ('SWITCH20', 'Switch', 'AMOUNT', 20, 12, '2026-01-01')");
        mockMvc.perform(get("/api/add-ons")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ROAM-1"));
        mockMvc.perform(get("/api/promotions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SWITCH20"));
    }
}
