package com.example.telecom.security;

import com.example.telecom.api.Times;
import com.example.telecom.audit.AuditAction;
import com.example.telecom.audit.AuditService;
import com.example.telecom.audit.AuditService.Actor;
import com.example.telecom.error.UnauthorizedException;
import com.example.telecom.model.AuditEvent.Outcome;
import com.example.telecom.model.StaffUser;
import com.example.telecom.repository.StaffUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Signs staff in and out. Repeated wrong passwords lock the account for a
 * while; unknown usernames take as long to reject as wrong passwords, so
 * response times do not reveal which usernames exist.
 */
@Service
public class LoginService {

    static final String INVALID_CREDENTIALS = "Invalid username or password.";
    private static final long SECONDS_PER_MINUTE = 60;

    private final StaffUserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository contextRepository;
    private final AuditService audit;
    private final int maxFailedLogins;
    private final Duration lockout;
    private final String unknownUserHash;

    public LoginService(StaffUserRepository users, PasswordEncoder encoder,
            SecurityContextRepository contextRepository, AuditService audit,
            @Value("${app.security.max-failed-logins:5}") int maxFailedLogins,
            @Value("${app.security.lockout-minutes:15}") long lockoutMinutes) {
        this.users = users;
        this.encoder = encoder;
        this.contextRepository = contextRepository;
        this.audit = audit;
        this.maxFailedLogins = maxFailedLogins;
        this.lockout = Duration.ofMinutes(lockoutMinutes);
        this.unknownUserHash = encoder.encode("unknown-user-placeholder-1");
    }

    public StaffUser login(String username, String password, HttpServletRequest request,
            HttpServletResponse response) {
        String name = username.trim();
        Instant now = Times.now();
        StaffUser user = users.findByUsernameIgnoreCase(name).orElse(null);
        if (user != null && user.isLocked(now)) {
            audit.record(Actor.of(user), AuditAction.LOGIN_FAILED, Outcome.FAILED, user.getId(),
                    "Sign-in refused: account locked");
            throw new UnauthorizedException(lockedMessage(user, now));
        }
        if (!passwordMatches(user, password)) {
            recordFailure(user, name, now);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            audit.record(Actor.of(user), AuditAction.LOGIN_FAILED, Outcome.FAILED, user.getId(),
                    "Sign-in refused: account disabled");
            throw new UnauthorizedException("This account is disabled. Ask an administrator to reactivate it.");
        }
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        StaffUser saved = users.save(user);
        startSession(saved, request, response, true);
        audit.record(Actor.of(saved), AuditAction.LOGIN, Outcome.SUCCESS, saved.getId(), "Signed in");
        return saved;
    }

    /** Always runs one hash check, also for unknown users, so timing reveals nothing. */
    private boolean passwordMatches(StaffUser user, String password) {
        String hash = user != null ? user.getPasswordHash() : unknownUserHash;
        boolean matches = PasswordPolicy.fitsHash(password) && encoder.matches(password, hash);
        return user != null && matches;
    }

    public void logout(HttpServletRequest request) {
        audit.record(Actor.current(), AuditAction.LOGOUT, Outcome.SUCCESS, Actor.current().id(), "Signed out");
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Stores the account in the session. A fresh sign-in also replaces the
     * session, so a session ID that existed before sign-in cannot be reused.
     */
    public void startSession(StaffUser user, HttpServletRequest request, HttpServletResponse response,
            boolean newSession) {
        if (newSession) {
            HttpSession old = request.getSession(false);
            if (old != null) {
                old.invalidate();
            }
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(StaffPrincipal.of(user).toAuthentication());
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    private void recordFailure(StaffUser user, String name, Instant now) {
        if (user == null) {
            audit.record(Actor.unverified(name), AuditAction.LOGIN_FAILED, Outcome.FAILED, null,
                    "Sign-in failed: unknown username");
            return;
        }
        int failures = user.getFailedLogins() + 1;
        if (failures >= maxFailedLogins) {
            user.setFailedLogins(0);
            user.setLockedUntil(now.plus(lockout));
            users.save(user);
            audit.record(Actor.of(user), AuditAction.LOGIN_FAILED, Outcome.FAILED, user.getId(),
                    "Sign-in failed: wrong password (" + failures + " of " + maxFailedLogins + ")");
            audit.record(Actor.of(user), AuditAction.ACCOUNT_LOCKED, Outcome.SUCCESS, user.getId(), "Locked for "
                    + lockout.toMinutes() + " minutes after " + maxFailedLogins + " failed sign-in attempts");
        } else {
            user.setFailedLogins(failures);
            users.save(user);
            audit.record(Actor.of(user), AuditAction.LOGIN_FAILED, Outcome.FAILED, user.getId(),
                    "Sign-in failed: wrong password (" + failures + " of " + maxFailedLogins + ")");
        }
    }

    private static String lockedMessage(StaffUser user, Instant now) {
        long seconds = Duration.between(now, user.getLockedUntil()).toSeconds();
        long minutes = Math.max(1, (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE);
        return "This account is locked after too many failed sign-in attempts. Try again in " + minutes
                + (minutes == 1 ? " minute" : " minutes") + " or ask an administrator to unlock it.";
    }
}
