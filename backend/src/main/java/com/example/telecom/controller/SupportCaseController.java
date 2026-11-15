package com.example.telecom.controller;

import com.example.telecom.api.PageResponse;
import com.example.telecom.api.Paging;
import com.example.telecom.api.SearchTerms;
import com.example.telecom.api.Times;
import com.example.telecom.error.ConflictException;
import com.example.telecom.error.FieldValidationException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.MobileLine;
import com.example.telecom.model.SupportCase;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.MobileLineRepository;
import com.example.telecom.repository.SupportCaseRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer support cases. Cases are kept as a service record and cannot be
 * deleted; a closed case cannot be changed, so follow-ups go in a new case.
 */
@RestController
@RequestMapping("/api/cases")
public class SupportCaseController {

    private static final Logger LOG = LoggerFactory.getLogger(SupportCaseController.class);
    private static final Set<SupportCase.Status> DONE = EnumSet.of(SupportCase.Status.RESOLVED,
            SupportCase.Status.CLOSED);

    private final SupportCaseRepository repository;
    private final CustomerRepository customerRepository;
    private final MobileLineRepository lineRepository;

    public SupportCaseController(SupportCaseRepository repository, CustomerRepository customerRepository,
            MobileLineRepository lineRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.lineRepository = lineRepository;
    }

    /** {@code q} matches the case number or subject. */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<CaseResponse> getCases(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String q,
            @RequestParam(required = false) SupportCase.Priority priority,
            @RequestParam(required = false) SupportCase.Status status,
            @RequestParam(required = false) SupportCase.Category category,
            @RequestParam(required = false) Long customerId) {
        SearchTerms terms = SearchTerms.of(q);
        Instant now = Times.now();
        return PageResponse.of(
                repository.findFiltered(priority, status, category, customerId, terms.text(), Paging.of(page, size)),
                c -> CaseResponse.from(c, now));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CaseResponse getCase(@PathVariable Long id) {
        return CaseResponse.from(findCase(id), Times.now());
    }

    /** Opens a case. The SLA due time follows from the priority. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CaseResponse createCase(@Valid @RequestBody CaseCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new FieldValidationException("customerId",
                        "customer " + request.customerId() + " does not exist"));
        SupportCase supportCase = new SupportCase();
        supportCase.setCustomer(customer);
        String msisdn = DeviceController.parseMsisdn(request.msisdn());
        if (msisdn != null) {
            supportCase.setLine(findCustomerLine(customer, msisdn));
        }
        supportCase.setCategory(request.category());
        supportCase.setChannel(request.channel());
        supportCase.setSubject(request.subject().trim());
        supportCase.setDescription(CustomerController.trimToNull(request.description()));
        supportCase.setPriority(request.priority());
        supportCase.setStatus(SupportCase.Status.OPEN);
        Instant now = Times.now();
        supportCase.setCreatedAt(now);
        supportCase.setSlaDueAt(now.plus(request.priority().slaHours(), ChronoUnit.HOURS));
        supportCase.setCaseNumber("CS" + (1_000_000 + repository.findMaxId() + 1));
        SupportCase saved = repository.save(supportCase);
        LOG.info("Case created: id={}, number={}", saved.getId(), saved.getCaseNumber());
        return CaseResponse.from(saved, now);
    }

    /**
     * Partial update. Resolving or closing a case records the resolution time;
     * moving a resolved case back to an open status clears it. Changing the
     * priority recalculates the SLA from the time the case was opened.
     */
    @PatchMapping("/{id}")
    @Transactional
    public CaseResponse updateCase(@PathVariable Long id, @Valid @RequestBody CaseUpdateRequest request) {
        SupportCase supportCase = findCase(id);
        if (supportCase.getStatus() == SupportCase.Status.CLOSED) {
            throw new ConflictException("Case " + supportCase.getCaseNumber()
                    + " is closed and cannot be changed. Open a new case instead.");
        }
        if (request.subject() != null) {
            supportCase.setSubject(request.subject().trim());
        }
        if (request.description() != null) {
            supportCase.setDescription(CustomerController.trimToNull(request.description()));
        }
        if (request.category() != null) {
            supportCase.setCategory(request.category());
        }
        if (request.priority() != null && request.priority() != supportCase.getPriority()) {
            supportCase.setPriority(request.priority());
            supportCase.setSlaDueAt(supportCase.getCreatedAt().plus(request.priority().slaHours(), ChronoUnit.HOURS));
        }
        Instant now = Times.now();
        if (request.status() != null) {
            applyStatus(supportCase, request.status(), now);
        }
        return CaseResponse.from(repository.save(supportCase), now);
    }

    private static void applyStatus(SupportCase supportCase, SupportCase.Status status, Instant now) {
        if (DONE.contains(status)) {
            if (supportCase.getResolvedAt() == null) {
                supportCase.setResolvedAt(now);
            }
        } else {
            supportCase.setResolvedAt(null);
        }
        supportCase.setStatus(status);
    }

    private MobileLine findCustomerLine(Customer customer, String number) {
        return lineRepository.findByMsisdn(number)
                .filter(line -> line.getAccount().getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new FieldValidationException("msisdn",
                        "line " + number + " does not belong to customer " + customer.getCustomerNumber()));
    }

    private SupportCase findCase(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Case " + id + " does not exist."));
    }

    public record CaseCreateRequest(@NotNull Long customerId,
            @Pattern(regexp = DeviceController.MSISDN_PATTERN, message = DeviceController.MSISDN_MESSAGE) String msisdn,
            @NotNull SupportCase.Category category, @NotNull SupportCase.Channel channel,
            @NotBlank @Size(max = 200) String subject, @Size(max = 4000) String description,
            @NotNull SupportCase.Priority priority) {
    }

    /** Partial update: fields left out or sent as null keep their current value. */
    public record CaseUpdateRequest(
            @Size(min = 1, max = 200) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String subject,
            @Size(max = 4000) String description, SupportCase.Category category, SupportCase.Priority priority,
            SupportCase.Status status) {
    }

    /**
     * {@code sla_breached} is true when the case was resolved after its due time,
     * or is still unresolved past it.
     */
    public record CaseResponse(Long id, String case_number, Long customer_id, String customer_number,
            String customer_name, Long line_id, String msisdn, String category, String channel, String subject,
            String description, String priority, String status, String sla_due_at, boolean sla_breached,
            String created_at, String resolved_at) {

        static CaseResponse from(SupportCase c, Instant now) {
            Customer customer = c.getCustomer();
            MobileLine line = c.getLine();
            Instant measuredAt = c.getResolvedAt() != null ? c.getResolvedAt() : now;
            return new CaseResponse(c.getId(), c.getCaseNumber(), customer.getId(), customer.getCustomerNumber(),
                    customer.displayName(), line != null ? line.getId() : null, line != null ? line.getMsisdn() : null,
                    c.getCategory().name(), c.getChannel().name(), c.getSubject(), c.getDescription(),
                    c.getPriority().name(), c.getStatus().name(), Times.iso(c.getSlaDueAt()),
                    measuredAt.isAfter(c.getSlaDueAt()), Times.iso(c.getCreatedAt()), Times.iso(c.getResolvedAt()));
        }
    }
}
