package com.example.telecom.repository;

import com.example.telecom.model.SupportCase;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportCaseRepository extends JpaRepository<SupportCase, Long> {
    boolean existsByCustomerId(Long customerId);

    boolean existsByLineId(Long lineId);

    long countByStatusIn(Collection<SupportCase.Status> statuses);

    @Query("SELECT COALESCE(MAX(c.id), 0) FROM SupportCase c")
    long findMaxId();

    @Query("""
            SELECT c FROM SupportCase c
            WHERE (:priority IS NULL OR c.priority = :priority)
              AND (:status IS NULL OR c.status = :status)
              AND (:category IS NULL OR c.category = :category)
              AND (:customerId IS NULL OR c.customer.id = :customerId)
              AND (:text IS NULL OR LOWER(c.caseNumber) LIKE :text ESCAPE '!'
                   OR LOWER(c.subject) LIKE :text ESCAPE '!')
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    Page<SupportCase> findFiltered(@Param("priority") SupportCase.Priority priority,
            @Param("status") SupportCase.Status status, @Param("category") SupportCase.Category category,
            @Param("customerId") Long customerId, @Param("text") String text, Pageable pageable);

    @Query("SELECT c.status, COUNT(c) FROM SupportCase c GROUP BY c.status ORDER BY c.status")
    List<Object[]> countGroupedByStatus();
}
