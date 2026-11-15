package com.example.telecom.controller;

import com.example.telecom.api.PageResponse;
import com.example.telecom.api.Paging;
import com.example.telecom.api.SearchTerms;
import com.example.telecom.api.Times;
import com.example.telecom.error.ConflictException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.IdentityNumbers;
import com.example.telecom.repository.AccountRepository;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.MobileLineRepository;
import com.example.telecom.repository.SupportCaseRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);
    static final String PHONE_PATTERN = "^\\+?[0-9 ()-]{8,20}$";
    static final String PHONE_MESSAGE = "must contain 8 to 20 digits, spaces, +, - or brackets";
    private static final String POSTAL_PATTERN = "^\\d{6}$";
    private static final String POSTAL_MESSAGE = "must be a 6-digit Singapore postal code";
    private static final String CUSTOMER = "Customer ";
    private static final String DOES_NOT_EXIST = " does not exist.";

    private final CustomerRepository repository;
    private final AccountRepository accountRepository;
    private final MobileLineRepository lineRepository;
    private final SupportCaseRepository caseRepository;

    public CustomerController(CustomerRepository repository, AccountRepository accountRepository,
            MobileLineRepository lineRepository, SupportCaseRepository caseRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.lineRepository = lineRepository;
        this.caseRepository = caseRepository;
    }

    /**
     * Lists customers, optionally filtered. {@code q} matches names, company,
     * email, customer number, UEN, account number or MSISDN. ID numbers are not
     * searchable.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String q,
            @RequestParam(required = false) Customer.Type type,
            @RequestParam(required = false) Customer.Status status) {
        LOG.info("Fetching customers");
        SearchTerms terms = SearchTerms.of(q);
        Page<Customer> result = repository.search(type, status, terms.text(), terms.digits(), Paging.of(page, size));
        Map<Long, Summary> summaries = summarize(result.getContent());
        return PageResponse.of(result, c -> CustomerResponse.from(c, summaries.get(c.getId()), false));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(@PathVariable Long id) {
        Customer customer = findCustomer(id);
        return CustomerResponse.from(customer, summarize(List.of(customer)).get(id), true);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        LOG.info("Creating {} customer", request.type());
        Customer customer = CustomerRules.newCustomer(request);
        ensureUnique(customer);
        customer.setCustomerNumber("C" + (1_000_000 + repository.findMaxId() + 1));
        Customer saved = repository.save(customer);
        LOG.info("Customer created: id={}", saved.getId());
        return CustomerResponse.from(saved, null, true);
    }

    /**
     * Partial update of contact details and status. Identity fields cannot change
     * once created.
     */
    @PatchMapping("/{id}")
    @Transactional
    public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        LOG.info("Updating customer: id={}", id);
        Customer customer = findCustomer(id);
        if (request.email() != null) {
            String email = request.email().trim();
            if (!email.equalsIgnoreCase(customer.getEmail()) && repository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException("A customer with the email " + email + " already exists.");
            }
        }
        CustomerRules.applyUpdate(customer, request);
        Customer saved = repository.save(customer);
        return CustomerResponse.from(saved, summarize(List.of(saved)).get(id), true);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        LOG.info("Deleting customer: id={}", id);
        if (!repository.existsById(id)) {
            throw new NotFoundException(CUSTOMER + id + DOES_NOT_EXIST);
        }
        if (accountRepository.existsByCustomerId(id) || caseRepository.existsByCustomerId(id)) {
            throw new ConflictException(
                    CUSTOMER + id + " has accounts or support cases. Set the status to CLOSED instead of deleting.");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void ensureUnique(Customer customer) {
        if (repository.existsByEmailIgnoreCase(customer.getEmail())) {
            throw new ConflictException("A customer with the email " + customer.getEmail() + " already exists.");
        }
        if (customer.getUen() != null && repository.existsByUenIgnoreCase(customer.getUen())) {
            throw new ConflictException("A business with the UEN " + customer.getUen() + " already exists.");
        }
        if (customer.getIdNumber() != null
                && repository.existsByIdTypeAndIdNumber(customer.getIdType(), customer.getIdNumber())) {
            throw new ConflictException("A customer with this ID number already exists. Search for the existing "
                    + "customer by name, email or phone number instead.");
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Map<Long, Summary> summarize(List<Customer> customers) {
        List<Long> ids = customers.stream().map(Customer::getId).toList();
        Map<Long, Summary> summaries = new HashMap<>();
        if (ids.isEmpty()) {
            return summaries;
        }
        for (Object[] row : accountRepository.summarizeByCustomer(ids)) {
            summaries.put((Long) row[0], new Summary(((Number) row[1]).longValue(), 0, 0, toMoney(row[2])));
        }
        for (Object[] row : lineRepository.summarizeByCustomer(ids)) {
            Summary accounts = summaries.getOrDefault((Long) row[0], Summary.EMPTY);
            summaries.put((Long) row[0], new Summary(accounts.accounts(), ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(), accounts.balance()));
        }
        return summaries;
    }

    static BigDecimal toMoney(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private Customer findCustomer(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(CUSTOMER + id + DOES_NOT_EXIST));
    }

    record Summary(long accounts, long lines, long activeLines, BigDecimal balance) {
        static final Summary EMPTY = new Summary(0, 0, 0, BigDecimal.ZERO.setScale(2));
    }

    public record CustomerCreateRequest(@NotNull Customer.Type type, @Size(max = 100) String firstName,
            @Size(max = 100) String lastName, @Size(max = 200) String companyName, @Size(max = 20) String uen,
            @Size(max = 200) String contactName, Customer.IdType idType, @Size(max = 20) String idNumber,
            LocalDate dateOfBirth, @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phone,
            @Size(max = 200) String addressLine,
            @Pattern(regexp = POSTAL_PATTERN, message = POSTAL_MESSAGE) String postalCode) {
    }

    /** Partial update: fields left out or sent as null keep their current value. */
    public record CustomerUpdateRequest(@Size(min = 1, max = 100) String firstName,
            @Size(min = 1, max = 100) String lastName, @Size(min = 1, max = 200) String companyName,
            @Size(max = 200) String contactName, @Email @Size(min = 3, max = 254) String email,
            @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phone, @Size(max = 200) String addressLine,
            @Pattern(regexp = POSTAL_PATTERN, message = POSTAL_MESSAGE) String postalCode, Customer.Status status) {
    }

    /**
     * ID numbers are only ever returned masked. Date of birth is only included in
     * single-customer responses.
     */
    public record CustomerResponse(Long id, String customer_number, String type, String display_name, String first_name,
            String last_name, String company_name, String uen, String contact_name, String id_type,
            String id_number_masked, String date_of_birth, String email, String phone, String address_line,
            String postal_code, String status, long account_count, long line_count, long active_line_count,
            BigDecimal total_balance, String created_at) {

        static CustomerResponse from(Customer c, Summary summary, boolean detail) {
            Summary s = summary != null ? summary : Summary.EMPTY;
            return new CustomerResponse(c.getId(), c.getCustomerNumber(), c.getType().name(), c.displayName(),
                    c.getFirstName(), c.getLastName(), c.getCompanyName(), c.getUen(), c.getContactName(),
                    c.getIdType() != null ? c.getIdType().name() : null, IdentityNumbers.mask(c.getIdNumber()),
                    detail ? Times.iso(c.getDateOfBirth()) : null, c.getEmail(), c.getPhone(), c.getAddressLine(),
                    c.getPostalCode(), c.getStatus().name(), s.accounts(), s.lines(), s.activeLines(), s.balance(),
                    Times.iso(c.getCreatedAt()));
        }
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String upper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }
}
