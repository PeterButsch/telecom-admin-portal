package com.example.telecom;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.config.RequestSizeLimitFilter;
import com.example.telecom.repository.StaffUserRepository;
import com.example.telecom.security.StaffPrincipal;
import com.example.telecom.security.StaffRole;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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
 * <li>one staff account per role, named after the role in lower case (for
 * example care_agent), all with the password {@link #STAFF_PASSWORD}</li>
 * </ul>
 * {@link #mockMvc} sends every request as the administrator with a valid CSRF
 * token; {@link #anonymousMvc} sends requests as they arrive from a browser.
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    protected static final String FIXTURE_TIME = "2026-02-01 09:00:00.000";
    protected static final String ADA_NRIC = "S1234567D";
    protected static final String STAFF_PASSWORD = "Test-Password-2026";
    // A low BCrypt cost keeps the fixture fast; the app accepts any cost.
    private static final String STAFF_PASSWORD_HASH = new BCryptPasswordEncoder(4).encode(STAFF_PASSWORD);
    private static final List<String> TABLES = List.of("usage_records", "support_cases", "devices", "line_add_ons",
            "mobile_lines", "accounts", "customers", "promotions", "add_ons", "plans", "staff_users");

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected StaffUserRepository staffUsers;

    protected MockMvc mockMvc;
    protected MockMvc anonymousMvc;
    private long auditBaseline;

    @BeforeEach
    void loadFixture() {
        String staff = "INSERT INTO staff_users (username, display_name, email, role, password_hash,"
                + " password_changed_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        for (StaffRole role : StaffRole.values()) {
            String username = username(role);
            jdbc.update(staff, username, role.label(), username + "@staff.test", role.name(), STAFF_PASSWORD_HASH,
                    FIXTURE_TIME, FIXTURE_TIME);
        }
        // csrf() in a test swaps in a session-based token repository. Put the
        // real cookie repository back so every test uses the production setup.
        WebTestUtils.setCsrfTokenRepository(new MockHttpServletRequest(context.getServletContext()),
                context.getBean(CookieCsrfTokenRepository.class));
        Long maxAudit = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM audit_events", Long.class);
        auditBaseline = maxAudit == null ? 0 : maxAudit;
        anonymousMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(RequestSizeLimitFilter.class)).apply(springSecurity()).build();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(RequestSizeLimitFilter.class))
                .apply(springSecurity()).defaultRequest(get("/").with(as(StaffRole.ADMIN)).with(csrf())).build();
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

    protected static String username(StaffRole role) {
        return role.name().toLowerCase(Locale.ROOT);
    }

    /** Signs the request in as the fixture account for the role. */
    protected RequestPostProcessor as(StaffRole role) {
        StaffPrincipal principal = StaffPrincipal.of(staffUsers.findByUsernameIgnoreCase(username(role)).orElseThrow());
        return authentication(principal.toAuthentication());
    }

    protected long staffId(StaffRole role) {
        return staffUsers.findByUsernameIgnoreCase(username(role)).orElseThrow().getId();
    }

    /** Signs in through the login endpoint, as a browser would. */
    protected ResultActions login(String username, String password) throws Exception {
        return anonymousMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    /** Signs in as the fixture account for the role and returns its session. */
    protected MockHttpSession signIn(StaffRole role) throws Exception {
        MvcResult result = login(username(role), STAFF_PASSWORD).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /** Audit events written since the test started, oldest first. */
    protected List<Map<String, Object>> auditEvents() {
        return jdbc.queryForList("SELECT * FROM audit_events WHERE id > ? ORDER BY id", auditBaseline);
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
