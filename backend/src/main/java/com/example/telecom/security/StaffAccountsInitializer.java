package com.example.telecom.security;

import com.example.telecom.api.Times;
import com.example.telecom.model.StaffUser;
import com.example.telecom.repository.StaffUserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first staff accounts when there are none: one demo account per
 * role when demo data is on, otherwise an {@code admin} account when
 * {@code app.security.bootstrap-admin-password} is set.
 */
@Component
public class StaffAccountsInitializer implements SmartInitializingSingleton {

    public static final List<DemoStaff> DEMO_STAFF = List.of(new DemoStaff("admin", "Grace Lim", StaffRole.ADMIN),
            new DemoStaff("agent", "Priya Nair", StaffRole.CARE_AGENT),
            new DemoStaff("teamlead", "Marcus Tan", StaffRole.CARE_TEAM_LEAD),
            new DemoStaff("fraud", "Siti Rahman", StaffRole.FRAUD_ANALYST),
            new DemoStaff("netops", "Daniel Ong", StaffRole.NETWORK_OPS),
            new DemoStaff("product", "Rachel Wong", StaffRole.PRODUCT_MANAGER));
    private static final Logger LOG = LoggerFactory.getLogger(StaffAccountsInitializer.class);

    private final StaffUserRepository users;
    private final PasswordEncoder encoder;
    private final boolean demoData;
    private final String demoPassword;
    private final String bootstrapPassword;

    public StaffAccountsInitializer(StaffUserRepository users, PasswordEncoder encoder,
            @Value("${app.demo-data.enabled:false}") boolean demoData,
            @Value("${app.demo-data.staff-password:}") String demoPassword,
            @Value("${app.security.bootstrap-admin-password:}") String bootstrapPassword) {
        this.users = users;
        this.encoder = encoder;
        this.demoData = demoData;
        this.demoPassword = demoPassword;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (users.count() > 0) {
            return;
        }
        if (demoData && !demoPassword.isBlank()) {
            PasswordPolicy.check("app.demo-data.staff-password", demoPassword, null);
            String hash = encoder.encode(demoPassword);
            for (DemoStaff staff : DEMO_STAFF) {
                users.save(newUser(staff.username(), staff.displayName(), staff.role(), hash));
            }
            LOG.info("Created {} demo staff accounts", DEMO_STAFF.size());
        } else if (bootstrapPassword.isBlank()) {
            LOG.warn("No staff accounts exist. Set APP_ADMIN_PASSWORD to create the first administrator.");
        } else {
            PasswordPolicy.check("app.security.bootstrap-admin-password", bootstrapPassword, "admin");
            users.save(newUser("admin", "Administrator", StaffRole.ADMIN, encoder.encode(bootstrapPassword)));
            LOG.info("Created the initial admin account");
        }
    }

    private static StaffUser newUser(String username, String displayName, StaffRole role, String hash) {
        StaffUser user = new StaffUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(username + "@staff.example.com");
        user.setRole(role);
        user.changePassword(hash, Times.now());
        return user;
    }

    /** A demo account created for one role. */
    public record DemoStaff(String username, String displayName, StaffRole role) {
    }
}
