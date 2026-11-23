package com.example.telecom.repository;

import com.example.telecom.model.LineAddOn;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LineAddOnRepository extends JpaRepository<LineAddOn, Long> {
    List<LineAddOn> findByLineIdAndEndedAtIsNullOrderById(Long lineId);

    @Query("""
            SELECT COALESCE(SUM(la.addOn.monthlyPrice), 0) FROM LineAddOn la
            WHERE la.endedAt IS NULL AND la.line.status = com.example.telecom.model.MobileLine.Status.ACTIVE
            """)
    BigDecimal sumActiveMonthlyCharges();
}
