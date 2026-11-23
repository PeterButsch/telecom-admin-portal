package com.example.telecom.repository;

import com.example.telecom.model.StaffUser;
import com.example.telecom.security.StaffRole;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-in bookkeeping uses single UPDATE statements rather than saving the
 * whole row: parallel sign-ins to one account then neither lose a failed
 * attempt nor overwrite each other's changes, and SQLite never has to upgrade a
 * read transaction to a write (which fails at once with SQLITE_BUSY).
 */
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {
    Optional<StaffUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndActiveTrue(StaffRole role);

    /**
     * Records a sign-in; returns 0, changing nothing, when the account is locked at
     * {@code now}.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE StaffUser u SET u.failedLogins = 0, u.lockedUntil = NULL, u.lastLoginAt = :now
            WHERE u.id = :id AND (u.lockedUntil IS NULL OR u.lockedUntil <= :now)
            """)
    int recordSignIn(@Param("id") Long id, @Param("now") Instant now);

    /**
     * Counts a wrong password; returns 0, changing nothing, when the account is
     * locked at {@code now}.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE StaffUser u SET u.failedLogins = u.failedLogins + 1
            WHERE u.id = :id AND (u.lockedUntil IS NULL OR u.lockedUntil <= :now)
            """)
    int recordFailedLogin(@Param("id") Long id, @Param("now") Instant now);

    @Query("SELECT u.failedLogins FROM StaffUser u WHERE u.id = :id")
    int findFailedLogins(@Param("id") Long id);

    /**
     * Locks the account once it has {@code max} failed sign-ins and resets the
     * count; returns 1 only for the request that applied the lock.
     */
    @Modifying
    @Transactional
    @Query("UPDATE StaffUser u SET u.failedLogins = 0, u.lockedUntil = :until WHERE u.id = :id AND u.failedLogins >= :max")
    int lockAfterFailedLogins(@Param("id") Long id, @Param("max") int max, @Param("until") Instant until);
}
