package com.example.telecom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.telecom.config.RequestSizeLimitFilter;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Runs the real application, including the request filter and exception
 * handler, against an in-memory SQLite database migrated by Flyway. Each test
 * starts from the same small fixture:
 * <ul>
 * <li>plan 1 TEST-5G (postpaid, $40, 24-month contract) and plan 2 TEST-PREPAID
 * ($18)</li>
 * <li>customer 1 Ada Tan (NRIC S1234567D) with postpaid account 1: line 1
 * 91234567 ACTIVE and line 2 81234567 TERMINATED</li>
 * <li>customer 2 Lion City Logistics Pte. Ltd. with account 2: line 3 98765432
 * ACTIVE</li>
 * </ul>
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    protected static final String FIXTURE_TIME = "2026-02-01 09:00:00.000";
    protected static final String ADA_NRIC = "S1234567D";
    private static final List<String> TABLES = List.of("usage_records", "support_cases", "devices", "line_add_ons",
            "mobile_lines", "accounts", "customers", "promotions", "add_ons", "plans");

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected JdbcTemplate jdbc;

    protected MockMvc mockMvc;

    @BeforeEach
    void loadFixture() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(RequestSizeLimitFilter.class))
                .build();
        String plan = "INSERT INTO plans (id, code, name, brand, category, segment, monthly_price, data_limit_gb,"
                + " contract_months, network, is_active, created_at) VALUES (?, ?, ?, ?, ?, 'CONSUMER', ?, 100, ?,"
                + " '5G', 1, ?)";
        jdbc.update(plan, 1, "TEST-5G", "Test 5G 100GB", "SINGTEL", "POSTPAID", 40.00, 24, FIXTURE_TIME);
        jdbc.update(plan, 2, "TEST-PREPAID", "Test Prepaid 100GB", "HI_CARD", "PREPAID", 18.00, 0, FIXTURE_TIME);
        jdbc.update("INSERT INTO customers (id, customer_number, type, first_name, last_name, id_type, id_number,"
                + " date_of_birth, email, phone, postal_code, status, created_at) VALUES (1, 'C1000001', 'INDIVIDUAL',"
                + " 'Ada', 'Tan', 'NRIC', ?, '1990-05-17', 'ada.tan@example.com', '+65 9123 4567', '560123',"
                + " 'ACTIVE', ?)", ADA_NRIC, FIXTURE_TIME);
        jdbc.update(
                "INSERT INTO customers (id, customer_number, type, company_name, uen, contact_name, email, phone,"
                        + " status, created_at) VALUES (2, 'C1000002', 'BUSINESS', 'Lion City Logistics Pte. Ltd.',"
                        + " '201912345K', 'Wei Ming Lim', 'telecom@lioncity.com.sg', '+65 6123 4567', 'ACTIVE', ?)",
                FIXTURE_TIME);
        String account = "INSERT INTO accounts (id, account_number, customer_id, type, bill_cycle_day, payment_method,"
                + " balance, credit_limit, status, created_at) VALUES (?, ?, ?, 'POSTPAID', 1, ?, 0, 500, 'ACTIVE', ?)";
        jdbc.update(account, 1, "1000000001", 1, "GIRO", FIXTURE_TIME);
        jdbc.update(account, 2, "1000000002", 2, "INVOICE", FIXTURE_TIME);
        String line = "INSERT INTO mobile_lines (id, account_id, plan_id, msisdn, sim_type, iccid, status, activated_at,"
                + " terminated_at, created_at) VALUES (?, ?, 1, ?, 'PHYSICAL', ?, ?, ?, ?, ?)";
        jdbc.update(line, 1, 1, "91234567", "8965010000000000001", "ACTIVE", FIXTURE_TIME, null, FIXTURE_TIME);
        jdbc.update(line, 2, 1, "81234567", "8965010000000000002", "TERMINATED", FIXTURE_TIME, FIXTURE_TIME,
                FIXTURE_TIME);
        jdbc.update(line, 3, 2, "98765432", "8965010000000000003", "ACTIVE", FIXTURE_TIME, null, FIXTURE_TIME);
    }

    @AfterEach
    void clearData() {
        for (String table : TABLES) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions patchJson(String url, String body) throws Exception {
        return mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions putJson(String url, String body) throws Exception {
        return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
