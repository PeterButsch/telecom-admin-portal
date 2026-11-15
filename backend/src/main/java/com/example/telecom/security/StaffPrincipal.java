package com.example.telecom.security;

import com.example.telecom.model.StaffUser;
import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The signed-in staff member as kept in the HTTP session. It carries no
 * password; {@code passwordChangedAt} lets the server end sessions that were
 * started before a password change.
 */
public record StaffPrincipal(Long id, String username, String displayName, StaffRole role,
        Instant passwordChangedAt) implements Principal, Serializable {

    private static final long serialVersionUID = 1L;

    public static StaffPrincipal of(StaffUser user) {
        return new StaffPrincipal(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                user.getPasswordChangedAt());
    }

    @Override
    public String getName() {
        return username;
    }

    /** Role authority ({@code ROLE_ADMIN}) plus one authority per permission. */
    public List<GrantedAuthority> authorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        for (Permission permission : role.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission.name()));
        }
        return authorities;
    }

    public Authentication toAuthentication() {
        return UsernamePasswordAuthenticationToken.authenticated(this, null, authorities());
    }

    /** True when the stored account still has this role and name. */
    boolean matches(StaffUser user) {
        return role == user.getRole() && Objects.equals(displayName, user.getDisplayName());
    }
}
