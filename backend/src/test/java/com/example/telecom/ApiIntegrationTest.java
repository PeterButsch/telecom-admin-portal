package com.example.telecom;

import com.example.telecom.model.Customer;
import com.example.telecom.model.Device;
import com.example.telecom.model.Plan;
import com.example.telecom.model.SupportTicket;
import com.example.telecom.model.UsageRecord;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.DeviceRepository;
import com.example.telecom.repository.PlanRepository;
import com.example.telecom.repository.SupportTicketRepository;
import com.example.telecom.repository.UsageRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real application against an in-memory SQLite database to check the
 * error handling and business rules end to end, including the request filter
 * and the exception handler.
 */
@SpringBootTest
class ApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private UsageRecordRepository usageRepository;

    private MockMvc mockMvc;
    private Plan plan;
    private Customer customer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(com.example.telecom.config.RequestSizeLimitFilter.class)).build();
        plan = planRepository.save(new Plan("Standard", new BigDecimal("40.00"), 10, 500, 500));
        customer = customerRepository.save(new Customer("Ada", "Lovelace", "ada@example.com", "555-0100", plan));
    }

    @AfterEach
    void tearDown() {
        usageRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        deviceRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
    }

    @Test
    void duplicateEmailIsAConflictNotAServerError() throws Exception {
        String body = """
                {"firstName":"Ada","lastName":"Again","email":"ADA@example.com","phone":"555-0199","planId":%d}
                """.formatted(plan.getId());

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEnumQueryParameterIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/tickets").param("priority", "CRITICAL")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/usage").param("type", "VIDEO")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidDateIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/usage").param("dateFrom", "2026-13-40")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/usage").param("dateFrom", "2026-02-02").param("dateTo", "2026-02-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchKeepsFieldsThatAreNotSent() throws Exception {
        mockMvc.perform(patch("/api/customers/" + customer.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"+65 9123 4567\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+65 9123 4567")).andExpect(jsonPath("$.first_name").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.plan_name").value("Standard"));
    }

    @Test
    void customerWithHistoryCannotBeDeletedSoUsageStaysReadable() throws Exception {
        ticketRepository.save(new SupportTicket(customer, "Billing", "Question", SupportTicket.Priority.LOW));
        usageRepository.save(new UsageRecord(customer, UsageRecord.Type.DATA, new BigDecimal("10.00"),
                new BigDecimal("0.10"), Instant.parse("2026-02-01T10:00:00Z")));

        mockMvc.perform(delete("/api/customers/" + customer.getId())).andExpect(status().isConflict());
        mockMvc.perform(get("/api/usage")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customer_name").value("Ada Lovelace"));
    }

    @Test
    void databaseRejectsRowsThatPointAtMissingRecords() {
        org.springframework.jdbc.core.JdbcTemplate jdbc = context
                .getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("INSERT INTO support_tickets (customer_id, subject, priority, status, created_at)"
                        + " VALUES (999999, 'Orphan', 'LOW', 'OPEN', CURRENT_TIMESTAMP)"));
    }

    @Test
    void planInUseCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/plans/" + plan.getId())).andExpect(status().isConflict());
    }

    @Test
    void planUpdateRequiresNameAndPrice() throws Exception {
        mockMvc.perform(put("/api/plans/" + plan.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataLimitGb\":20}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists()).andExpect(jsonPath("$.errors.monthlyPrice").exists());
    }

    @Test
    void reopeningATicketClearsResolvedAt() throws Exception {
        SupportTicket ticket = ticketRepository
                .save(new SupportTicket(customer, "No signal", "Since today", SupportTicket.Priority.HIGH));
        String url = "/api/tickets/" + ticket.getId();

        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolved_at").isNotEmpty());
        mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolved_at").value(nullValue()))
                .andExpect(jsonPath("$.subject").value("No signal"));
    }

    @Test
    void ticketForUnknownCustomerExplainsTheProblem() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":999999,\"subject\":\"Help\",\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Customer 999999 does not exist."));
    }

    @Test
    void deviceForUnknownCustomerIsRejected() throws Exception {
        mockMvc.perform(post("/api/devices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"imei\":\"351234567890123\",\"model\":\"Pixel\",\"simNumber\":\"89650100000000000001\","
                        + "\"customerId\":999999}"))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(0, deviceRepository.count());
    }

    @Test
    void deviceCannotBeAvailableAndAssignedAtTheSameTime() throws Exception {
        Device device = deviceRepository.save(new Device("351234567890123", "Pixel", "89650100000000000001"));

        mockMvc.perform(patch("/api/devices/" + device.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"AVAILABLE\",\"customerId\":" + customer.getId() + "}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/devices/" + device.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":" + customer.getId() + "}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mockMvc.perform(patch("/api/devices/" + device.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"AVAILABLE\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.customer_id").value(nullValue())).andExpect(jsonPath("$.model").value("Pixel"));
    }

    @Test
    void usageDateToIncludesTheWholeDayButNotTheNextMidnight() throws Exception {
        usageRepository.save(new UsageRecord(customer, UsageRecord.Type.SMS, BigDecimal.ONE, new BigDecimal("0.02"),
                Instant.parse("2026-02-01T23:59:00Z")));
        usageRepository.save(new UsageRecord(customer, UsageRecord.Type.SMS, BigDecimal.ONE, new BigDecimal("0.02"),
                Instant.parse("2026-02-02T00:00:00Z")));

        mockMvc.perform(get("/api/usage").param("dateTo", "2026-02-01")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void usageDateFiltersMatchSeededTextTimestampsAndAppWrittenOnes() throws Exception {
        org.springframework.jdbc.core.JdbcTemplate jdbc = context
                .getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        String insert = "INSERT INTO usage_records (customer_id, type, quantity, cost, recorded_at)"
                + " VALUES (?, 'DATA', 1, 0.10, ?)";
        jdbc.update(insert, customer.getId(), "2026-01-31 23:59:59.999");
        jdbc.update(insert, customer.getId(), "2026-02-01 08:00:00.000");
        jdbc.update(insert, customer.getId(), "2026-02-02 00:00:00.000");
        usageRepository.save(new UsageRecord(customer, UsageRecord.Type.SMS, BigDecimal.ONE, new BigDecimal("0.02"),
                Instant.parse("2026-02-01T12:00:00Z")));

        org.junit.jupiter.api.Assertions.assertEquals("2026-02-01 12:00:00.000",
                jdbc.queryForObject("SELECT recorded_at FROM usage_records WHERE type = 'SMS'", String.class));
        mockMvc.perform(get("/api/usage").param("dateFrom", "2026-02-01").param("dateTo", "2026-02-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].recorded_at").value("2026-02-01T12:00:00Z"));
    }

    @Test
    void healthReportsTheDatabase() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.database").value("up"));
    }

    @Test
    void oversizedRequestBodyIsRejected() throws Exception {
        String big = "{\"firstName\":\"" + "x".repeat(5000) + "\"}";

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(big))
                .andExpect(status().isPayloadTooLarge());
    }
}
