package com.example.telecom.repository;

import com.example.telecom.model.Account;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByCustomerIdOrderById(Long customerId);

    boolean existsByCustomerId(Long customerId);

    boolean existsByAccountNumber(String accountNumber);

    /** Rows of customer id, number of accounts and total balance. */
    @Query("SELECT a.customer.id, COUNT(a), SUM(a.balance) FROM Account a WHERE a.customer.id IN :ids GROUP BY a.customer.id")
    List<Object[]> summarizeByCustomer(@Param("ids") Collection<Long> ids);
}
