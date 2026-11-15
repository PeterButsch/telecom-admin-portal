package com.example.telecom.controller;

import com.example.telecom.error.BadRequestException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.SupportTicket;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.SupportTicketRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class SupportTicketController {

    private static final Logger LOG = LoggerFactory.getLogger(SupportTicketController.class);

    private final SupportTicketRepository repository;
    private final CustomerRepository customerRepository;

    public SupportTicketController(SupportTicketRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public PagedResponse getTickets(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) SupportTicket.Priority priority,
            @RequestParam(required = false) SupportTicket.Status status,
            @RequestParam(required = false) Long customerId) {
        LOG.info("Fetching tickets");
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        Page<SupportTicket> result;

        boolean hasFilters = priority != null || status != null || customerId != null;
        if (hasFilters) {
            result = repository.findFiltered(priority, status, customerId, pageRequest);
        } else {
            result = repository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        LOG.debug("Found {} tickets (page {} of {})", result.getNumberOfElements(), result.getNumber(),
                result.getTotalPages());
        return new PagedResponse(result.getContent().stream().map(TicketResponse::from).toList(), result.getNumber(),
                result.getTotalPages(), result.getTotalElements(), result.hasNext());
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(@PathVariable Long id) {
        LOG.info("Fetching ticket with id={}", id);
        return TicketResponse.from(findTicket(id));
    }

    @PostMapping
    public TicketResponse createTicket(@Valid @RequestBody TicketCreateRequest request) {
        LOG.info("Creating ticket");
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new BadRequestException("Customer " + request.customerId() + " does not exist."));
        SupportTicket ticket = new SupportTicket(customer, request.subject().trim(), request.description(),
                request.priority());
        SupportTicket saved = repository.save(ticket);
        LOG.info("Ticket created: id={}", saved.getId());
        return TicketResponse.from(saved);
    }

    /** Partial update: fields left out or sent as null keep their current value. */
    @PatchMapping("/{id}")
    public TicketResponse updateTicket(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest request) {
        LOG.info("Updating ticket: id={}", id);
        SupportTicket ticket = findTicket(id);
        if (request.subject() != null) {
            ticket.setSubject(request.subject().trim());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.status() != null) {
            applyStatus(ticket, request.status());
        }
        SupportTicket saved = repository.save(ticket);
        LOG.info("Ticket updated: id={}, status={}", saved.getId(), saved.getStatus());
        return TicketResponse.from(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        LOG.info("Deleting ticket: id={}", id);
        if (!repository.existsById(id)) {
            throw new NotFoundException("Ticket " + id + " does not exist.");
        }
        repository.deleteById(id);
        LOG.info("Ticket deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Keeps resolved_at consistent with the status: it records when the ticket was
     * first resolved or closed, and is cleared when the ticket is reopened.
     */
    static void applyStatus(SupportTicket ticket, SupportTicket.Status newStatus) {
        boolean wasDone = isDone(ticket.getStatus());
        boolean nowDone = isDone(newStatus);
        ticket.setStatus(newStatus);
        if (nowDone && (!wasDone || ticket.getResolvedAt() == null)) {
            ticket.setResolvedAt(Instant.now());
        } else if (!nowDone) {
            ticket.setResolvedAt(null);
        }
    }

    private static boolean isDone(SupportTicket.Status status) {
        return status == SupportTicket.Status.RESOLVED || status == SupportTicket.Status.CLOSED;
    }

    private SupportTicket findTicket(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Ticket " + id + " does not exist."));
    }

    public record TicketCreateRequest(@NotNull Long customerId, @NotBlank @Size(max = 200) String subject,
            @Size(max = 5000) String description, @NotNull SupportTicket.Priority priority) {
    }

    public record TicketUpdateRequest(@Size(min = 1, max = 200) String subject, @Size(max = 5000) String description,
            SupportTicket.Priority priority, SupportTicket.Status status) {
    }

    public record PagedResponse(List<TicketResponse> content, int page, int totalPages, long totalElements,
            boolean hasNext) {
    }

    public record TicketResponse(Long id, Long customer_id, String customer_name, String subject, String description,
            String priority, String status, String created_at, String resolved_at) {
        public static TicketResponse from(SupportTicket ticket) {
            return new TicketResponse(ticket.getId(), ticket.getCustomer().getId(),
                    ticket.getCustomer().getFirstName() + " " + ticket.getCustomer().getLastName(), ticket.getSubject(),
                    ticket.getDescription(), ticket.getPriority().name(), ticket.getStatus().name(),
                    ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null,
                    ticket.getResolvedAt() != null ? ticket.getResolvedAt().toString() : null);
        }
    }
}
