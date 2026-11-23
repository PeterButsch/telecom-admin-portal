package com.example.telecom.repository;

import com.example.telecom.model.AuditEvent;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE (:action IS NULL OR e.action = :action)
              AND (:entityType IS NULL OR e.entityType = :entityType)
              AND (:entityId IS NULL OR e.entityId = :entityId)
              AND (:outcome IS NULL OR e.outcome = :outcome)
              AND (:text IS NULL OR LOWER(e.summary) LIKE :text ESCAPE '!'
                   OR LOWER(e.actorUsername) LIKE :text ESCAPE '!')
              AND (:from IS NULL OR e.occurredAt >= :from)
              AND (:to IS NULL OR e.occurredAt < :to)
            ORDER BY e.occurredAt DESC, e.id DESC
            """)
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    Page<AuditEvent> findFiltered(@Param("action") String action, @Param("entityType") String entityType,
            @Param("entityId") String entityId, @Param("outcome") AuditEvent.Outcome outcome,
            @Param("text") String text, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
