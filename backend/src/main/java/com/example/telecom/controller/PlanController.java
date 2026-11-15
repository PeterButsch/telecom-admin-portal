package com.example.telecom.controller;

import com.example.telecom.error.ConflictException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.Plan;
import com.example.telecom.repository.CustomerRepository;
import com.example.telecom.repository.PlanRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private static final Logger LOG = LoggerFactory.getLogger(PlanController.class);

    private final PlanRepository repository;
    private final CustomerRepository customerRepository;

    public PlanController(PlanRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<PlanResponse> getAllPlans() {
        LOG.info("Fetching all plans");
        List<PlanResponse> plans = repository.findAll(Sort.by("id")).stream().map(PlanResponse::from).toList();
        LOG.debug("Found {} plans", plans.size());
        return plans;
    }

    @GetMapping("/active")
    public List<PlanResponse> getActivePlans() {
        LOG.info("Fetching active plans");
        List<PlanResponse> plans = repository.findByIsActiveTrue().stream().map(PlanResponse::from).toList();
        LOG.debug("Found {} active plans", plans.size());
        return plans;
    }

    @GetMapping("/{id}")
    public PlanResponse getPlan(@PathVariable Long id) {
        LOG.info("Fetching plan with id={}", id);
        return PlanResponse.from(findPlan(id));
    }

    @PostMapping
    public PlanResponse createPlan(@Valid @RequestBody PlanRequest request) {
        LOG.info("Creating plan");
        Plan plan = new Plan(request.name().trim(), request.monthlyPrice(), request.dataLimitGb(),
                request.minutesLimit(), request.smsLimit());
        if (request.isActive() != null) {
            plan.setIsActive(request.isActive());
        }
        Plan saved = repository.save(plan);
        LOG.info("Plan created: id={}", saved.getId());
        return PlanResponse.from(saved);
    }

    /**
     * Full replacement of the plan. A null limit means unlimited, so the caller
     * must send every field; isActive is the only field that keeps its value when
     * omitted.
     */
    @PutMapping("/{id}")
    public PlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        LOG.info("Updating plan: id={}", id);
        Plan plan = findPlan(id);
        plan.setName(request.name().trim());
        plan.setMonthlyPrice(request.monthlyPrice());
        plan.setDataLimitGb(request.dataLimitGb());
        plan.setMinutesLimit(request.minutesLimit());
        plan.setSmsLimit(request.smsLimit());
        if (request.isActive() != null) {
            plan.setIsActive(request.isActive());
        }
        Plan saved = repository.save(plan);
        LOG.info("Plan updated: id={}", saved.getId());
        return PlanResponse.from(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        LOG.info("Deleting plan: id={}", id);
        if (!repository.existsById(id)) {
            throw new NotFoundException("Plan " + id + " does not exist.");
        }
        long subscribers = customerRepository.countByPlanId(id);
        if (subscribers > 0) {
            throw new ConflictException("Plan " + id + " is used by " + subscribers
                    + " customer(s). Mark it inactive instead of deleting it.");
        }
        repository.deleteById(id);
        LOG.info("Plan deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    private Plan findPlan(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Plan " + id + " does not exist."));
    }

    public record PlanRequest(@NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal monthlyPrice,
            @PositiveOrZero Integer dataLimitGb, @PositiveOrZero Integer minutesLimit, @PositiveOrZero Integer smsLimit,
            Boolean isActive) {
    }

    public record PlanResponse(Long id, String name, BigDecimal monthly_price, Integer data_limit_gb,
            Integer minutes_limit, Integer sms_limit, Boolean is_active) {
        public static PlanResponse from(Plan plan) {
            return new PlanResponse(plan.getId(), plan.getName(), plan.getMonthlyPrice(), plan.getDataLimitGb(),
                    plan.getMinutesLimit(), plan.getSmsLimit(), plan.getIsActive());
        }
    }
}
