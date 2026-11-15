package com.example.telecom.controller;

import com.example.telecom.api.Times;
import com.example.telecom.error.BadRequestException;
import com.example.telecom.error.ConflictException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.MobileLine;
import com.example.telecom.model.Plan;
import com.example.telecom.repository.MobileLineRepository;
import com.example.telecom.repository.PlanRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private static final Logger LOG = LoggerFactory.getLogger(PlanController.class);
    private static final Set<Integer> CONTRACT_TERMS = Set.of(0, 12, 24);

    private final PlanRepository repository;
    private final MobileLineRepository lineRepository;

    public PlanController(PlanRepository repository, MobileLineRepository lineRepository) {
        this.repository = repository;
        this.lineRepository = lineRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        Map<Long, Long> lines = liveLineCounts();
        return repository.findAll(Sort.by("brand", "monthlyPrice", "id")).stream()
                .map(p -> PlanResponse.from(p, lines.getOrDefault(p.getId(), 0L))).toList();
    }

    @GetMapping("/active")
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        Map<Long, Long> lines = liveLineCounts();
        return repository.findByActiveTrueOrderByBrandAscMonthlyPriceAsc().stream()
                .map(p -> PlanResponse.from(p, lines.getOrDefault(p.getId(), 0L))).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PlanResponse getPlan(@PathVariable Long id) {
        return PlanResponse.from(findPlan(id), liveLineCount(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PlanResponse createPlan(@Valid @RequestBody PlanRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A plan with the code " + code + " already exists.");
        }
        Plan plan = new Plan();
        plan.setCode(code);
        apply(plan, request);
        Plan saved = repository.save(plan);
        LOG.info("Plan created: id={}", saved.getId());
        return PlanResponse.from(saved, 0);
    }

    /**
     * Full replacement of the plan except its code. A null limit means unlimited,
     * so the caller must send every field; isActive keeps its value when omitted.
     */
    @PutMapping("/{id}")
    @Transactional
    public PlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        Plan plan = findPlan(id);
        if (!plan.getCode().equalsIgnoreCase(request.code().trim())) {
            throw new BadRequestException("The plan code cannot change; create a new plan instead.");
        }
        apply(plan, request);
        Plan saved = repository.save(plan);
        LOG.info("Plan updated: id={}", saved.getId());
        return PlanResponse.from(saved, liveLineCount(id));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Plan " + id + " does not exist.");
        }
        long lines = lineRepository.countByPlanId(id);
        if (lines > 0) {
            throw new ConflictException(
                    "Plan " + id + " is used by " + lines + " line(s). Mark it inactive instead of deleting it.");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static void apply(Plan plan, PlanRequest request) {
        int contract = request.contractMonths() != null ? request.contractMonths() : 0;
        if (!CONTRACT_TERMS.contains(contract)) {
            throw new BadRequestException("contractMonths must be 0, 12 or 24.");
        }
        if (request.category() == Plan.Category.PREPAID && contract != 0) {
            throw new BadRequestException("Prepaid plans cannot have a contract.");
        }
        plan.setName(request.name().trim());
        plan.setBrand(request.brand());
        plan.setCategory(request.category());
        plan.setSegment(request.segment());
        plan.setMonthlyPrice(request.monthlyPrice());
        plan.setDataLimitGb(request.dataLimitGb());
        plan.setMinutesLimit(request.minutesLimit());
        plan.setSmsLimit(request.smsLimit());
        plan.setContractMonths(contract);
        plan.setNetwork(request.network() != null ? request.network() : "5G");
        if (request.isActive() != null) {
            plan.setActive(request.isActive());
        }
    }

    private long liveLineCount(Long planId) {
        return lineRepository.countByPlanIdAndStatusNot(planId, MobileLine.Status.TERMINATED);
    }

    private Map<Long, Long> liveLineCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : lineRepository.countLiveLinesByPlan()) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Plan findPlan(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Plan " + id + " does not exist."));
    }

    public record PlanRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "may only contain letters, digits and -") String code,
            @NotBlank @Size(max = 100) String name, @NotNull Plan.Brand brand, @NotNull Plan.Category category,
            @NotNull Plan.Segment segment,
            @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal monthlyPrice,
            @PositiveOrZero Integer dataLimitGb, @PositiveOrZero Integer minutesLimit, @PositiveOrZero Integer smsLimit,
            Integer contractMonths, @Pattern(regexp = "^[45]G$", message = "must be 4G or 5G") String network,
            Boolean isActive) {
    }

    /** {@code line_count} counts lines on the plan that are not terminated. */
    public record PlanResponse(Long id, String code, String name, String brand, String category, String segment,
            BigDecimal monthly_price, Integer data_limit_gb, Integer minutes_limit, Integer sms_limit,
            int contract_months, String network, boolean is_active, long line_count, String created_at) {
        static PlanResponse from(Plan p, long lineCount) {
            return new PlanResponse(p.getId(), p.getCode(), p.getName(), p.getBrand().name(), p.getCategory().name(),
                    p.getSegment().name(), CustomerController.toMoney(p.getMonthlyPrice()), p.getDataLimitGb(),
                    p.getMinutesLimit(), p.getSmsLimit(), p.getContractMonths(), p.getNetwork(), p.isActive(),
                    lineCount, Times.iso(p.getCreatedAt()));
        }
    }
}
