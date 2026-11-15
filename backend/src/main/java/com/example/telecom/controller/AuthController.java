package com.example.telecom.controller;

import com.example.telecom.api.Times;
import com.example.telecom.audit.AuditAction;
import com.example.telecom.audit.AuditService;
import com.example.telecom.error.FieldValidationException;
import com.example.telecom.error.UnauthorizedException;
import com.example.telecom.model.StaffUser;
import com.example.telecom.repository.StaffUserRepository;
import com.example.telecom.security.LoginService;
import com.example.telecom.security.PasswordPolicy;
import com.example.telecom.security.Permission;
import com.example.telecom.security.StaffAccountsInitializer;
import com.example.telecom.security.StaffPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff sign-in. The session lives in an HttpOnly cookie; state-changing
 * requests must echo the XSRF-TOKEN cookie in an X-XSRF-TOKEN header.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final StaffUserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final boolean demoData;
    private final String demoPassword;

    public AuthController(LoginService loginService, StaffUserRepository users, PasswordEncoder encoder,
            AuditService audit, @Value("${app.demo-data.enabled:false}") boolean demoData,
            @Value("${app.demo-data.staff-password:}") String demoPassword) {
        this.loginService = loginService;
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
        this.demoData = demoData;
        this.demoPassword = demoPassword;
    }

    /** Returns the CSRF token; the response also sets it as the XSRF-TOKEN cookie. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/login")
    public SessionUser login(@Valid @RequestBody LoginRequest login, HttpServletRequest request,
            HttpServletResponse response) {
        return SessionUser.from(loginService.login(login.username(), login.password(), request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        loginService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public SessionUser me(@AuthenticationPrincipal StaffPrincipal principal) {
        return SessionUser.from(currentUser(principal));
    }

    /**
     * Changes the signed-in user's own password. Their other sessions end; this
     * one continues.
     */
    @PostMapping("/password")
    @Transactional
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody PasswordChangeRequest change, HttpServletRequest request,
            HttpServletResponse response) {
        StaffUser user = currentUser(principal);
        if (!encoder.matches(change.currentPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("currentPassword", "is incorrect");
        }
        PasswordPolicy.check("newPassword", change.newPassword(), user.getUsername());
        if (encoder.matches(change.newPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("newPassword", "must be different from the current password");
        }
        user.changePassword(encoder.encode(change.newPassword()), Times.now());
        StaffUser saved = users.save(user);
        loginService.startSession(saved, request, response, false);
        audit.record(AuditAction.PASSWORD_CHANGED, saved.getId(), "Changed own password");
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists the demo accounts and their shared password so evaluators can sign
     * in; empty unless demo data is enabled.
     */
    @GetMapping("/demo-accounts")
    @Transactional(readOnly = true)
    public DemoAccounts demoAccounts() {
        if (!demoData || demoPassword.isBlank()) {
            return new DemoAccounts(null, List.of());
        }
        List<DemoAccount> accounts = StaffAccountsInitializer.DEMO_STAFF.stream()
                .map(staff -> users.findByUsernameIgnoreCase(staff.username()).orElse(null))
                .filter(user -> user != null && user.isActive())
                .map(user -> new DemoAccount(user.getUsername(), user.getDisplayName(), user.getRole().name(),
                        user.getRole().label()))
                .toList();
        return new DemoAccounts(demoPassword, accounts);
    }

    private StaffUser currentUser(StaffPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sign in to continue.");
        }
        return users.findById(principal.id()).orElseThrow(() -> new UnauthorizedException("Sign in to continue."));
    }

    public record LoginRequest(@NotBlank @Size(max = 100) String username,
            @NotNull @Size(max = 200) String password) {
    }

    public record PasswordChangeRequest(@NotNull @Size(max = 200) String currentPassword,
            @NotNull @Size(max = 200) String newPassword) {
    }

    /** The signed-in staff member and everything their role allows. */
    public record SessionUser(Long id, String username, String display_name, String email, String role,
            String role_label, List<String> permissions) {
        public SessionUser {
            permissions = List.copyOf(permissions);
        }

        static SessionUser from(StaffUser user) {
            return new SessionUser(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                    user.getRole().name(), user.getRole().label(),
                    user.getRole().permissions().stream().map(Permission::name).sorted().toList());
        }
    }

    public record DemoAccount(String username, String display_name, String role, String role_label) {
    }

    public record DemoAccounts(String password, List<DemoAccount> accounts) {
        public DemoAccounts {
            accounts = List.copyOf(accounts);
        }
    }
}
