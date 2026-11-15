package com.example.telecom.repository;

import com.example.telecom.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUenIgnoreCase(String uen);

    boolean existsByIdTypeAndIdNumber(Customer.IdType idType, String idNumber);

    long countByStatus(Customer.Status status);

    @Query("SELECT COALESCE(MAX(c.id), 0) FROM Customer c")
    long findMaxId();

    /**
     * Searches by name, company, email, customer number or UEN, and by MSISDN or
     * account number when the search contains digits. ID numbers (NRIC/FIN) are
     * deliberately not searchable.
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE (:type IS NULL OR c.type = :type)
              AND (:status IS NULL OR c.status = :status)
              AND (:text IS NULL
                   OR LOWER(CONCAT(COALESCE(c.firstName, ''), ' ', COALESCE(c.lastName, ''))) LIKE :text
                   OR LOWER(COALESCE(c.companyName, '')) LIKE :text
                   OR LOWER(c.email) LIKE :text
                   OR LOWER(c.customerNumber) LIKE :text
                   OR LOWER(COALESCE(c.uen, '')) LIKE :text
                   OR (:digits IS NOT NULL AND EXISTS (
                        SELECT 1 FROM Account a WHERE a.customer = c AND a.accountNumber LIKE :digits))
                   OR (:digits IS NOT NULL AND EXISTS (
                        SELECT 1 FROM MobileLine l WHERE l.account.customer = c AND l.msisdn LIKE :digits)))
            ORDER BY c.id
            """)
    Page<Customer> search(@Param("type") Customer.Type type, @Param("status") Customer.Status status,
            @Param("text") String text, @Param("digits") String digits, Pageable pageable);
}
