package com.example.telecom.controller;

import com.example.telecom.api.PageResponse;
import com.example.telecom.api.Paging;
import com.example.telecom.api.Times;
import com.example.telecom.error.BadRequestException;
import com.example.telecom.model.Customer;
import com.example.telecom.model.MobileLine;
import com.example.telecom.model.UsageRecord;
import com.example.telecom.repository.UsageRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rated usage per mobile line. Usage is written by the network mediation feed,
 * so this API is read-only.
 */
@RestController
@RequestMapping("/api/usage")
public class UsageRecordController {

    private final UsageRecordRepository repository;

    public UsageRecordController(UsageRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists usage newest first. dateFrom and dateTo are inclusive calendar days in
     * UTC.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<UsageRecordResponse> getUsageRecords(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size, @RequestParam(required = false) UsageRecord.Type type,
            @RequestParam(required = false) Long lineId, @RequestParam(required = false) String msisdn,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be on or before dateTo.");
        }
        String number = DeviceController.parseMsisdn(msisdn);
        Instant from = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toExclusive = dateTo != null ? dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        return PageResponse.of(
                repository.findFiltered(type, lineId, number, customerId, from, toExclusive, Paging.of(page, size)),
                UsageRecordResponse::from);
    }

    /**
     * Quantity is in {@code unit}: minutes for calls, megabytes for data, messages
     * for SMS.
     */
    public record UsageRecordResponse(Long id, Long line_id, String msisdn, Long customer_id, String customer_name,
            String type, BigDecimal quantity, String unit, BigDecimal cost, boolean roaming, String recorded_at) {

        static UsageRecordResponse from(UsageRecord u) {
            MobileLine line = u.getLine();
            Customer customer = line.getAccount().getCustomer();
            return new UsageRecordResponse(u.getId(), line.getId(), line.getMsisdn(), customer.getId(),
                    customer.displayName(), u.getType().name(), u.getQuantity(), unit(u.getType()),
                    CustomerController.toMoney(u.getCost()), u.isRoaming(), Times.iso(u.getRecordedAt()));
        }

        private static String unit(UsageRecord.Type type) {
            return switch (type) {
                case CALL -> "MIN";
                case DATA -> "MB";
                case SMS -> "SMS";
            };
        }
    }
}
