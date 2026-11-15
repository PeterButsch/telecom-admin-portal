package com.example.telecom.repository;

import com.example.telecom.model.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByActiveTrueOrderByBrandAscMonthlyPriceAsc();

    boolean existsByCodeIgnoreCase(String code);
}
