package com.example.telecom.controller;

import com.example.telecom.error.BadRequestException;
import com.example.telecom.model.UsageRecord;
import com.example.telecom.repository.UsageRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usage")
public class UsageRecordController {

    private static final Logger LOG = LoggerFactory.getLogger(UsageRecordController.class);

    private final UsageRecordRepository repository;

    public UsageRecordController(UsageRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists usage records newest first. dateFrom and dateTo are inclusive calendar
     * days in UTC.
     */
    @GetMapping
    public PagedResponse getUsageRecords(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) UsageRecord.Type type,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        LOG.info("Fetching usage records");
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be on or before dateTo.");
        }
        Instant from = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toExclusive = dateTo != null ? dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        Page<UsageRecord> result;

        boolean hasFilters = type != null || customerId != null || from != null || toExclusive != null;
        if (hasFilters) {
            result = repository.findFiltered(type, customerId, from, toExclusive, pageRequest);
        } else {
            result = repository.findAllByOrderByRecordedAtDesc(pageRequest);
        }

        LOG.debug("Found {} usage records (page {} of {})", result.getNumberOfElements(), result.getNumber(),
                result.getTotalPages());
        return PagedResponse.from(result);
    }

    @GetMapping("/customer/{customerId}")
    public PagedResponse getByCustomer(@PathVariable Long customerId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        LOG.info("Fetching usage records for customer: customerId={}", customerId);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        return PagedResponse.from(repository.findByCustomerIdOrderByRecordedAtDesc(customerId, pageRequest));
    }

    public record PagedResponse(List<UsageRecordResponse> content, int page, int totalPages, long totalElements,
            boolean hasNext) {
        static PagedResponse from(Page<UsageRecord> result) {
            return new PagedResponse(result.getContent().stream().map(UsageRecordResponse::from).toList(),
                    result.getNumber(), result.getTotalPages(), result.getTotalElements(), result.hasNext());
        }
    }

    public record UsageRecordResponse(Long id, Long customer_id, String customer_name, String type, BigDecimal quantity,
            BigDecimal cost, String recorded_at) {
        public static UsageRecordResponse from(UsageRecord record) {
            return new UsageRecordResponse(record.getId(), record.getCustomer().getId(),
                    record.getCustomer().getFirstName() + " " + record.getCustomer().getLastName(),
                    record.getType().name(), record.getQuantity(), record.getCost(),
                    record.getRecordedAt() != null ? record.getRecordedAt().toString() : null);
        }
    }
}
