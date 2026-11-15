package com.example.telecom.controller;

import com.example.telecom.error.BadRequestException;
import com.example.telecom.error.ConflictException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.Plan;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.DeviceRepository;
import com.example.telecom.repository.PlanRepository;
import com.example.telecom.repository.SupportTicketRepository;
import com.example.telecom.repository.UsageRecordRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);
    private static final String PHONE_PATTERN = "^[+0-9 ()-]{6,20}$";
    private static final String PHONE_MESSAGE = "must contain 6 to 20 digits, spaces, +, - or brackets";

    private final CustomerRepository repository;
    private final PlanRepository planRepository;
    private final DeviceRepository deviceRepository;
    private final SupportTicketRepository ticketRepository;
    private final UsageRecordRepository usageRepository;

    public CustomerController(CustomerRepository repository, PlanRepository planRepository,
            DeviceRepository deviceRepository, SupportTicketRepository ticketRepository,
            UsageRecordRepository usageRepository) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.deviceRepository = deviceRepository;
        this.ticketRepository = ticketRepository;
        this.usageRepository = usageRepository;
    }

    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        LOG.info("Fetching all customers");
        List<CustomerResponse> customers = repository.findAll(Sort.by("id")).stream().map(CustomerResponse::from)
                .toList();
        LOG.debug("Found {} customers", customers.size());
        return customers;
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomer(@PathVariable Long id) {
        LOG.info("Fetching customer with id={}", id);
        return CustomerResponse.from(findCustomer(id));
    }

    @PostMapping
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        LOG.info("Creating customer");
        Plan plan = findPlan(request.planId());
        String email = request.email().trim();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A customer with the email " + email + " already exists.");
        }
        Customer customer = new Customer(request.firstName().trim(), request.lastName().trim(), email,
                request.phone().trim(), plan);
        if (request.balance() != null) {
            customer.setBalance(request.balance());
        }
        Customer saved = repository.save(customer);
        LOG.info("Customer created: id={}", saved.getId());
        return CustomerResponse.from(saved);
    }

    @PatchMapping("/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        LOG.info("Updating customer: id={}", id);
        Customer customer = findCustomer(id);
        applyContactChanges(customer, request);
        applyAccountChanges(customer, request);
        Customer saved = repository.save(customer);
        LOG.info("Customer updated: id={}", saved.getId());
        return CustomerResponse.from(saved);
    }

    private void applyContactChanges(Customer customer, CustomerUpdateRequest request) {
        if (request.firstName() != null) {
            customer.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            customer.setLastName(request.lastName().trim());
        }
        if (request.email() != null) {
            String email = request.email().trim();
            if (!email.equalsIgnoreCase(customer.getEmail()) && repository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException("A customer with the email " + email + " already exists.");
            }
            customer.setEmail(email);
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone().trim());
        }
    }

    private void applyAccountChanges(Customer customer, CustomerUpdateRequest request) {
        if (request.planId() != null) {
            customer.setPlan(findPlan(request.planId()));
        }
        if (request.status() != null) {
            customer.setStatus(request.status());
        }
        if (request.balance() != null) {
            customer.setBalance(request.balance());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        LOG.info("Deleting customer: id={}", id);
        if (!repository.existsById(id)) {
            throw new NotFoundException("Customer " + id + " does not exist.");
        }
        if (deviceRepository.existsByCustomerId(id) || ticketRepository.existsByCustomerId(id)
                || usageRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer " + id
                    + " has devices, support tickets or usage history. Set the status to CANCELLED instead of deleting.");
        }
        repository.deleteById(id);
        LOG.info("Customer deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    private Customer findCustomer(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Customer " + id + " does not exist."));
    }

    private Plan findPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new BadRequestException("Plan " + planId + " does not exist."));
    }

    public record CustomerCreateRequest(@NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName, @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phone, @NotNull Long planId,
            @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal balance) {
    }

    /** Partial update: fields left out or sent as null keep their current value. */
    public record CustomerUpdateRequest(@Size(min = 1, max = 100) String firstName,
            @Size(min = 1, max = 100) String lastName, @Email @Size(min = 3, max = 254) String email,
            @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phone, Long planId, Customer.Status status,
            @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal balance) {
    }

    public record CustomerResponse(Long id, String first_name, String last_name, String email, String phone,
            Long plan_id, String plan_name, String status, BigDecimal balance, String activated_at, String created_at) {
        public static CustomerResponse from(Customer customer) {
            return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(),
                    customer.getEmail(), customer.getPhone(),
                    customer.getPlan() != null ? customer.getPlan().getId() : null,
                    customer.getPlan() != null ? customer.getPlan().getName() : null, customer.getStatus().name(),
                    customer.getBalance(),
                    customer.getActivatedAt() != null ? customer.getActivatedAt().toString() : null,
                    customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null);
        }
    }
}
