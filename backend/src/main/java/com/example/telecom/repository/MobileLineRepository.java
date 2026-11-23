package com.example.telecom.repository;

import com.example.telecom.model.MobileLine;
import com.example.telecom.model.Plan;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobileLineRepository extends JpaRepository<MobileLine, Long> {
    Optional<MobileLine> findByMsisdn(String msisdn);

    List<MobileLine> findByAccountIdOrderById(Long accountId);

    long countByPlanId(Long planId);

    long countByPlanIdAndStatusNot(Long planId, MobileLine.Status status);

    long countByStatus(MobileLine.Status status);

    boolean existsByMsisdn(String msisdn);

    boolean existsByIccid(String iccid);

    @Query("""
            SELECT l FROM MobileLine l
            WHERE (:status IS NULL OR l.status = :status)
              AND (:brand IS NULL OR l.plan.brand = :brand)
              AND (:customerId IS NULL OR l.account.customer.id = :customerId)
              AND (:accountId IS NULL OR l.account.id = :accountId)
              AND (:text IS NULL
                   OR (:names = TRUE
                       AND (LOWER(CONCAT(COALESCE(l.account.customer.firstName, ''), ' ',
                                         COALESCE(l.account.customer.lastName, ''))) LIKE :text ESCAPE '!'
                            OR LOWER(COALESCE(l.account.customer.companyName, '')) LIKE :text ESCAPE '!'
                            OR LOWER(COALESCE(l.userName, '')) LIKE :text ESCAPE '!'))
                   OR (:digits IS NOT NULL AND (l.msisdn LIKE :digits OR l.iccid LIKE :digits
                                                OR l.account.accountNumber LIKE :digits)))
            ORDER BY l.id
            """)
    Page<MobileLine> search(@Param("status") MobileLine.Status status, @Param("brand") Plan.Brand brand,
            @Param("customerId") Long customerId, @Param("accountId") Long accountId, @Param("text") String text,
            @Param("names") boolean names, @Param("digits") String digits, Pageable pageable);

    /** Rows of customer id, number of lines and number of active lines. */
    @Query("""
            SELECT l.account.customer.id, COUNT(l),
                   SUM(CASE WHEN l.status = com.example.telecom.model.MobileLine.Status.ACTIVE THEN 1 ELSE 0 END)
            FROM MobileLine l WHERE l.account.customer.id IN :ids GROUP BY l.account.customer.id
            """)
    List<Object[]> summarizeByCustomer(@Param("ids") Collection<Long> ids);

    @Query("SELECT l.plan.id, COUNT(l) FROM MobileLine l WHERE l.status <> com.example.telecom.model.MobileLine.Status.TERMINATED GROUP BY l.plan.id")
    List<Object[]> countLiveLinesByPlan();

    @Query("""
            SELECT l.plan.name, COUNT(l) FROM MobileLine l
            WHERE l.status = com.example.telecom.model.MobileLine.Status.ACTIVE
            GROUP BY l.plan.name ORDER BY COUNT(l) DESC
            """)
    List<Object[]> countActiveByPlanName();

    @Query("""
            SELECT l.plan.name, SUM(l.plan.monthlyPrice) FROM MobileLine l
            WHERE l.status = com.example.telecom.model.MobileLine.Status.ACTIVE
            GROUP BY l.plan.name ORDER BY SUM(l.plan.monthlyPrice) DESC
            """)
    List<Object[]> sumActiveRevenueByPlanName();

    @Query("SELECT COUNT(DISTINCT l.account.customer.id) FROM MobileLine l WHERE l.status = com.example.telecom.model.MobileLine.Status.ACTIVE")
    long countCustomersWithActiveLines();
}
