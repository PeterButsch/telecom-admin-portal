package com.example.telecom.repository;

import com.example.telecom.model.UsageRecord;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    boolean existsByLineId(Long lineId);

    @Query("""
            SELECT u FROM UsageRecord u
            WHERE (:type IS NULL OR u.type = :type)
              AND (:lineId IS NULL OR u.line.id = :lineId)
              AND (:msisdn IS NULL OR u.line.msisdn = :msisdn)
              AND (:customerId IS NULL OR u.line.account.customer.id = :customerId)
              AND (:dateFrom IS NULL OR u.recordedAt >= :dateFrom)
              AND (:dateToExclusive IS NULL OR u.recordedAt < :dateToExclusive)
            ORDER BY u.recordedAt DESC, u.id DESC
            """)
    Page<UsageRecord> findFiltered(@Param("type") UsageRecord.Type type, @Param("lineId") Long lineId,
            @Param("msisdn") String msisdn, @Param("customerId") Long customerId, @Param("dateFrom") Instant dateFrom,
            @Param("dateToExclusive") Instant dateToExclusive, Pageable pageable);
}
