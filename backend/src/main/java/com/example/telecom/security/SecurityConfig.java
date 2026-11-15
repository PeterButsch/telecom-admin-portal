package com.example.telecom.security;

import static com.example.telecom.security.Permission.AUDIT_READ;
import static com.example.telecom.security.Permission.CASE_READ;
import static com.example.telecom.security.Permission.CASE_WRITE;
import static com.example.telecom.security.Permission.CATALOG_READ;
import static com.example.telecom.security.Permission.CUSTOMER_DELETE;
import static com.example.telecom.security.Permission.CUSTOMER_READ;
import static com.example.telecom.security.Permission.CUSTOMER_WRITE;
import static com.example.telecom.security.Permission.DASHBOARD_READ;
import static com.example.telecom.security.Permission.DEVICE_READ;
import static com.example.telecom.security.Permission.DEVICE_WRITE;
import static com.example.telecom.security.Permission.LINE_READ;
import static com.example.telecom.security.Permission.PLAN_WRITE;
import static com.example.telecom.security.Permission.STAFF_ADMIN;
import static com.example.telecom.security.Permission.USAGE_READ;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;

import com.example.telecom.audit.AuditAction;
import com.example.telecom.audit.AuditService;
import com.example.telecom.model.AuditEvent;
import com.example.telecom.repository.StaffUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Session sign-in with CSRF protection, and the permission each API endpoint
 * needs. Endpoints that are not listed here are denied, so a new endpoint
 * cannot become reachable by accident.
 */
@Configuration
@EnableWebSecurity
@SuppressWarnings("PMD.TooManyStaticImports")
public class SecurityConfig {

    static final String CSRF_TITLE = "Invalid CSRF token";
    private static final String CUSTOMERS = "/api/customers/**";
    private static final String ACCOUNTS = "/api/accounts/**";
    private static final String CASES = "/api/cases/**";
    private static final String PLANS = "/api/plans/**";
    private static final String DEVICES = "/api/devices/**";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(@Value("${app.security.secure-cookies:false}") boolean secure) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.secure(secure).sameSite("Lax"));
        return repository;
    }

    @Bean
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    SecurityFilterChain securityFilterChain(HttpSecurity http, CookieCsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository contextRepository, StaffUserRepository staffUsers, AuditService audit)
            throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(new SessionRevalidationFilter(staffUsers, contextRepository),
                        SecurityContextHolderFilter.class)
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint())
                        .accessDeniedHandler(accessDeniedHandler(audit)))
                .authorizeHttpRequests(SecurityConfig::authorize);
        return http.build();
    }

    private static void authorize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
        rules.requestMatchers("/error", "/api/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/demo-accounts").permitAll()
                .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .authenticated().requestMatchers(GET, CUSTOMERS, ACCOUNTS).hasAuthority(CUSTOMER_READ.name())
                .requestMatchers(DELETE, CUSTOMERS).hasAuthority(CUSTOMER_DELETE.name())
                .requestMatchers(CUSTOMERS, ACCOUNTS).hasAuthority(CUSTOMER_WRITE.name())
                .requestMatchers(GET, "/api/lines/**").hasAuthority(LINE_READ.name()).requestMatchers(GET, CASES)
                .hasAuthority(CASE_READ.name()).requestMatchers(CASES).hasAuthority(CASE_WRITE.name())
                .requestMatchers(GET, PLANS, "/api/add-ons/**", "/api/promotions/**").hasAuthority(CATALOG_READ.name())
                .requestMatchers(PLANS).hasAuthority(PLAN_WRITE.name()).requestMatchers(GET, DEVICES)
                .hasAuthority(DEVICE_READ.name()).requestMatchers(DEVICES).hasAuthority(DEVICE_WRITE.name())
                .requestMatchers(GET, "/api/usage/**").hasAuthority(USAGE_READ.name())
                .requestMatchers(GET, "/api/dashboard/**").hasAuthority(DASHBOARD_READ.name())
                .requestMatchers(GET, "/api/audit-events/**").hasAuthority(AUDIT_READ.name())
                .requestMatchers("/api/staff-users/**").hasAuthority(STAFF_ADMIN.name()).anyRequest().denyAll();
    }

    private static AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> ProblemResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized", "Sign in to continue.");
    }

    private static AccessDeniedHandler accessDeniedHandler(AuditService audit) {
        return (request, response, ex) -> {
            if (ex instanceof CsrfException) {
                ProblemResponses.write(response, HttpServletResponse.SC_FORBIDDEN, CSRF_TITLE,
                        "The security token for this page is missing or out of date. Reload the page and try again.");
                return;
            }
            String target = request.getMethod() + " " + request.getRequestURI();
            audit.record(AuditService.Actor.current(), AuditAction.ACCESS_DENIED, AuditEvent.Outcome.DENIED, target,
                    "Denied " + target);
            ProblemResponses.write(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                    "Your role does not allow this action.");
        };
    }
}
