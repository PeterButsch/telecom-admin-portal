package com.example.telecom.controller;

import com.example.telecom.error.GlobalExceptionHandler;
import com.example.telecom.model.Customer;
import com.example.telecom.model.Plan;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.DeviceRepository;
import com.example.telecom.repository.PlanRepository;
import com.example.telecom.repository.SupportTicketRepository;
import com.example.telecom.repository.UsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final SupportTicketRepository ticketRepository = mock(SupportTicketRepository.class);
    private final UsageRecordRepository usageRepository = mock(UsageRecordRepository.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new CustomerController(repository, planRepository, deviceRepository, ticketRepository, usageRepository))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void getAllCustomersFlattensPlanOntoTheResponse() throws Exception {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(customer(1L, plan(2L, "Standard", "40.00"))));

        mockMvc.perform(get("/api/customers")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].first_name").value("First1"))
                .andExpect(jsonPath("$[0].last_name").value("Last1")).andExpect(jsonPath("$[0].plan_id").value(2))
                .andExpect(jsonPath("$[0].plan_name").value("Standard"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getAllCustomersToleratesAMissingPlan() throws Exception {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(customer(1L, null)));

        mockMvc.perform(get("/api/customers")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plan_id").value(nullValue()))
                .andExpect(jsonPath("$[0].plan_name").value(nullValue()));
    }

    @Test
    void getCustomerReturnsNotFoundWithAProblemBody() throws Exception {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/customers/404")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Customer 404 does not exist."));
    }

    @Test
    void createCustomerRejectsAnUnknownPlan() throws Exception {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        String body = """
                {"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"555-0100","planId":99}
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Plan 99 does not exist."));

        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void createCustomerRejectsMissingAndMalformedFields() throws Exception {
        String body = """
                {"firstName":"","email":"not-an-email","phone":"abc","planId":2}
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists()).andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists());

        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void createCustomerRejectsADuplicateEmailWithConflict() throws Exception {
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan(2L, "Standard", "40.00")));
        when(repository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        String body = """
                {"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"555-0100","planId":2}
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void createCustomerStartsActiveWithTheGivenBalance() throws Exception {
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan(2L, "Standard", "40.00")));
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        String body = """
                {"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"555-0100",
                 "planId":2,"balance":12.34}
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.balance").value(12.34))
                .andExpect(jsonPath("$.plan_name").value("Standard"));
    }

    @Test
    void updateCustomerOnlyChangesTheFieldsItIsGiven() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(customer(1L, plan(2L, "Standard", "40.00"))));
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(
                patch("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.first_name").value("First1"))
                .andExpect(jsonPath("$.email").value("customer1@example.com"))
                .andExpect(jsonPath("$.phone").value("555-0001")).andExpect(jsonPath("$.plan_name").value("Standard"));
    }

    @Test
    void updateCustomerRejectsAnUnknownStatus() throws Exception {
        mockMvc.perform(
                patch("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void updateCustomerReturnsNotFoundForUnknownId() throws Exception {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(
                patch("/api/customers/404").contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"Ada\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCustomerReturnsNoContentWhenThereIsNoHistory() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/customers/1")).andExpect(status().isNoContent());

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteCustomerIsRefusedWhenTheCustomerHasHistory() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);
        when(ticketRepository.existsByCustomerId(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/customers/1")).andExpect(status().isConflict());

        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void deleteCustomerReturnsNotFoundWhenMissing() throws Exception {
        when(repository.existsById(404L)).thenReturn(false);

        mockMvc.perform(delete("/api/customers/404")).andExpect(status().isNotFound());

        verify(repository, never()).deleteById(anyLong());
    }

    private static Plan plan(Long id, String name, String price) {
        Plan plan = new Plan(name, new BigDecimal(price), 10, 500, 500);
        plan.setId(id);
        return plan;
    }

    private static Customer customer(Long id, Plan plan) {
        Customer customer = new Customer("First" + id, "Last" + id, "customer" + id + "@example.com", "555-000" + id,
                plan);
        customer.setId(id);
        return customer;
    }
}
