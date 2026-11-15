package com.example.telecom.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsLongPasswordsWithLettersAndDigits() {
        assertNull(PasswordPolicy.problem("Correct-Horse-42", "agent"));
    }

    @Test
    void rejectsWeakPasswords() {
        assertEquals("must be at least 12 characters", PasswordPolicy.problem("Short-1", "agent"));
        assertEquals("must contain both letters and digits", PasswordPolicy.problem("only-letters-here", "agent"));
        assertEquals("must contain both letters and digits", PasswordPolicy.problem("123456789012", "agent"));
        assertEquals("must not contain the username", PasswordPolicy.problem("my-AGENT-pass-1", "agent"));
        assertEquals("must be at most 72 bytes", PasswordPolicy.problem("a1".repeat(40), "agent"));
    }

    @Test
    void everyRoleCanReadTheDashboardAndOnlyAdminsManageStaff() {
        for (StaffRole role : StaffRole.values()) {
            assertTrue(role.has(Permission.DASHBOARD_READ), role.name());
            assertEquals(role == StaffRole.ADMIN, role.has(Permission.STAFF_ADMIN), role.name());
        }
    }
}
