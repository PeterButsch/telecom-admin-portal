package com.example.telecom.controller;

import com.example.telecom.api.PageResponse;
import com.example.telecom.api.Paging;
import com.example.telecom.api.SearchTerms;
import com.example.telecom.api.Times;
import com.example.telecom.audit.AuditAction;
import com.example.telecom.error.BadRequestException;
import com.example.telecom.model.AuditEvent;
import com.example.telecom.repository.AuditEventRepository;
import com.example.telecom.security.StaffRole;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only access to the audit trail; there is no API to change it. */
@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {

    private final AuditEventRepository repository;

    public AuditEventController(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists events newest first. {@code q} matches the summary or username;
     * dateFrom and dateTo are inclusive calendar days in UTC.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> getAuditEvents(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) String q,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditAction.Entity entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) AuditEvent.Outcome outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be on or before dateTo.");
        }
        return PageResponse.of(repository.findFiltered(name(action), name(entityType),
                CustomerController.trimToNull(entityId), outcome, SearchTerms.of(q).text(), startOf(dateFrom),
                startOf(dateTo != null ? dateTo.plusDays(1) : null), Paging.of(page, size)), AuditEventResponse::from);
    }

    private static String name(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private static Instant startOf(LocalDate day) {
        return day != null ? day.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
    }

    /** The values the action and entity type filters accept. */
    @GetMapping("/filters")
    public AuditFilters getFilters() {
        return new AuditFilters(Arrays.stream(AuditAction.values()).map(Enum::name).toList(),
                Arrays.stream(AuditAction.Entity.values()).map(Enum::name).toList());
    }

    public record AuditFilters(List<String> actions, List<String> entity_types) {
        public AuditFilters {
            actions = List.copyOf(actions);
            entity_types = List.copyOf(entity_types);
        }
    }

    public record AuditEventResponse(Long id, String occurred_at, Long actor_id, String actor_username,
            String actor_role, String actor_role_label, String action, String entity_type, String entity_id,
            String summary, String outcome) {
        static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(e.getId(), Times.iso(e.getOccurredAt()), e.getActorId(),
                    e.getActorUsername(), e.getActorRole(), roleLabel(e.getActorRole()), e.getAction(),
                    e.getEntityType(), e.getEntityId(), e.getSummary(), e.getOutcome().name());
        }

        private static String roleLabel(String role) {
            return Arrays.stream(StaffRole.values()).filter(r -> r.name().equals(role)).map(StaffRole::label)
                    .findFirst().orElse(null);
        }
    }
}
