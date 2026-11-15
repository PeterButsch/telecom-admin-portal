package com.example.telecom.controller;

import com.example.telecom.api.Times;
import com.example.telecom.audit.AuditAction;
import com.example.telecom.audit.AuditService;
import com.example.telecom.audit.ChangeSet;
import com.example.telecom.error.BadRequestException;
import com.example.telecom.error.ConflictException;
import com.example.telecom.error.NotFoundException;
import com.example.telecom.model.StaffUser;
import com.example.telecom.repository.StaffUserRepository;
import com.example.telecom.security.PasswordPolicy;
import com.example.telecom.security.StaffPrincipal;
import com.example.telecom.security.StaffRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff account administration. Accounts are deactivated rather than deleted
 * so the audit trail keeps pointing at real people.
 */
@RestController
@RequestMapping("/api/staff-users")
public class StaffUserController {

    private static final String USERNAME_PATTERN = "^[a-z0-9._-]{3,40}$";
    private static final String USERNAME_MESSAGE = "must be 3 to 40 lower-case letters, digits, '.', '_' or '-'";

    private final StaffUserRepository repository;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public StaffUserController(StaffUserRepository repository, PasswordEncoder encoder, AuditService audit) {
        this.repository = repository;
        this.encoder = encoder;
        this.audit = audit;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<StaffUserResponse> getStaffUsers() {
        Instant now = Times.now();
        return repository.findAll(Sort.by("displayName", "id")).stream().map(u -> StaffUserResponse.from(u, now))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StaffUserResponse createStaffUser(@Valid @RequestBody StaffUserCreateRequest request) {
        if (repository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("The username " + request.username() + " is already taken.");
        }
        String email = request.email().trim();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Another staff account already uses " + email + ".");
        }
        PasswordPolicy.check("password", request.password(), request.username());
        StaffUser user = new StaffUser();
        user.setUsername(request.username());
        user.setDisplayName(request.displayName().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.changePassword(encoder.encode(request.password()), Times.now());
        StaffUser saved = repository.save(user);
        audit.record(AuditAction.STAFF_USER_CREATED, saved.getId(),
                "Created " + saved.getUsername() + " as " + saved.getRole().label());
        return StaffUserResponse.from(saved, Times.now());
    }

    /**
     * Partial update of name, email, role and active flag. Administrators cannot
     * change their own role or deactivate themselves, and the last active
     * administrator cannot be removed.
     */
    @PatchMapping("/{id}")
    @Transactional
    public StaffUserResponse updateStaffUser(@PathVariable Long id, @Valid @RequestBody StaffUserUpdateRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        StaffUser user = findUser(id);
        checkAccessChange(user, request, principal);
        ChangeSet changes = applyChanges(user, request);
        StaffUser saved = repository.save(user);
        if (!changes.isEmpty()) {
            audit.record(AuditAction.STAFF_USER_UPDATED, id, "Updated " + saved.getUsername() + ": " + changes.describe());
        }
        return StaffUserResponse.from(saved, Times.now());
    }

    private void checkAccessChange(StaffUser user, StaffUserUpdateRequest request, StaffPrincipal principal) {
        boolean removesAccess = request.role() != null && request.role() != user.getRole()
                || Boolean.FALSE.equals(request.active()) && user.isActive();
        if (!removesAccess) {
            return;
        }
        if (principal != null && Objects.equals(principal.id(), user.getId())) {
            throw new BadRequestException("You cannot change your own role or deactivate your own account.");
        }
        if (user.isActive() && user.getRole() == StaffRole.ADMIN
                && repository.countByRoleAndActiveTrue(StaffRole.ADMIN) <= 1) {
            throw new ConflictException("At least one active administrator is required.");
        }
    }

    private ChangeSet applyChanges(StaffUser user, StaffUserUpdateRequest request) {
        ChangeSet changes = new ChangeSet();
        if (request.displayName() != null) {
            changes.value("name", user.getDisplayName(), request.displayName().trim());
            user.setDisplayName(request.displayName().trim());
        }
        if (request.email() != null) {
            String email = request.email().trim();
            if (!email.equalsIgnoreCase(user.getEmail()) && repository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException("Another staff account already uses " + email + ".");
            }
            changes.value("email", user.getEmail(), email);
            user.setEmail(email);
        }
        if (request.role() != null) {
            changes.value("role", user.getRole().label(), request.role().label());
            user.setRole(request.role());
        }
        if (request.active() != null) {
            changes.value("active", user.isActive(), request.active());
            user.setActive(request.active());
        }
        return changes;
    }

    @PostMapping("/{id}/unlock")
    @Transactional
    public StaffUserResponse unlock(@PathVariable Long id) {
        StaffUser user = findUser(id);
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        StaffUser saved = repository.save(user);
        audit.record(AuditAction.STAFF_USER_UNLOCKED, id, "Unlocked " + saved.getUsername());
        return StaffUserResponse.from(saved, Times.now());
    }

    /**
     * Sets a new password chosen by the administrator. It ends the user's
     * sessions and clears any lock.
     */
    @PostMapping("/{id}/password")
    @Transactional
    public StaffUserResponse resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        StaffUser user = findUser(id);
        PasswordPolicy.check("password", request.password(), user.getUsername());
        Instant now = Times.now();
        user.changePassword(encoder.encode(request.password()), now);
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        StaffUser saved = repository.save(user);
        audit.record(AuditAction.STAFF_PASSWORD_RESET, id, "Reset the password of " + saved.getUsername());
        return StaffUserResponse.from(saved, now);
    }

    private StaffUser findUser(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff account " + id + " does not exist."));
    }

    public record StaffUserCreateRequest(
            @NotBlank @Pattern(regexp = USERNAME_PATTERN, message = USERNAME_MESSAGE) String username,
            @NotBlank @Size(max = 100) String displayName, @NotBlank @Email @Size(max = 254) String email,
            @NotNull StaffRole role, @NotNull @Size(max = 200) String password) {
    }

    /** Partial update: fields left out or sent as null keep their current value. */
    public record StaffUserUpdateRequest(
            @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String displayName,
            @Email @Size(min = 3, max = 254) String email, StaffRole role, Boolean active) {
    }

    public record PasswordResetRequest(@NotNull @Size(max = 200) String password) {
    }

    public record StaffUserResponse(Long id, String username, String display_name, String email, String role,
            String role_label, boolean is_active, boolean is_locked, String locked_until, int failed_logins,
            String last_login_at, String created_at) {
        static StaffUserResponse from(StaffUser u, Instant now) {
            boolean locked = u.isLocked(now);
            return new StaffUserResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getEmail(),
                    u.getRole().name(), u.getRole().label(), u.isActive(), locked,
                    locked ? Times.iso(u.getLockedUntil()) : null, u.getFailedLogins(), Times.iso(u.getLastLoginAt()),
                    Times.iso(u.getCreatedAt()));
        }
    }
}
