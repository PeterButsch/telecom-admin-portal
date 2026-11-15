package com.example.telecom.security;

import com.example.telecom.model.StaffUser;
import com.example.telecom.repository.StaffUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks the signed-in account on every request, so that deactivating a user
 * or resetting their password ends their sessions at once, and a role change
 * applies to their next request.
 */
final class SessionRevalidationFilter extends OncePerRequestFilter {

    private final StaffUserRepository users;
    private final SecurityContextRepository contextRepository;

    SessionRevalidationFilter(StaffUserRepository users, SecurityContextRepository contextRepository) {
        super();
        this.users = users;
        this.contextRepository = contextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof StaffPrincipal principal) {
            StaffUser user = users.findById(principal.id()).orElse(null);
            if (user == null || !user.isActive() || !principal.passwordChangedAt().equals(user.getPasswordChangedAt())) {
                endSession(request);
            } else if (!principal.matches(user)) {
                SecurityContext refreshed = SecurityContextHolder.createEmptyContext();
                refreshed.setAuthentication(StaffPrincipal.of(user).toAuthentication());
                SecurityContextHolder.setContext(refreshed);
                contextRepository.saveContext(refreshed, request, response);
            }
        }
        chain.doFilter(request, response);
    }

    private static void endSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
