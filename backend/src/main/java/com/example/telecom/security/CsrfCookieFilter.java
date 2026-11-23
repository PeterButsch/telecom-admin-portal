package com.example.telecom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security creates CSRF tokens lazily; reading the token on every
 * request makes sure the browser always has the XSRF-TOKEN cookie that the
 * frontend echoes back in the X-XSRF-TOKEN header.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
        chain.doFilter(request, response);
    }
}
