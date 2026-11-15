package com.example.telecom;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.security.StaffRole;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class AuthApiTest extends IntegrationTestSupport {

    private static final String INVALID = "Invalid username or password.";
    private static final String ME = "/api/auth/me";

    @Test
    void anonymousRequestsMustSignIn() throws Exception {
        anonymousMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Sign in to continue."));
        anonymousMvc.perform(get(ME)).andExpect(status().isUnauthorized());
        anonymousMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void signInStartsASessionWithTheRolePermissions() throws Exception {
        login("CARE_AGENT", STAFF_PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("care_agent"))
                .andExpect(jsonPath("$.role").value("CARE_AGENT"))
                .andExpect(jsonPath("$.role_label").value("Care Agent"))
                .andExpect(jsonPath("$.permissions", hasItem("CUSTOMER_WRITE")))
                .andExpect(jsonPath("$.permissions", not(hasItem("AUDIT_READ"))))
                .andExpect(jsonPath("$.password_hash").doesNotExist());
        MockHttpSession session = signIn(StaffRole.CARE_AGENT);
        anonymousMvc.perform(get(ME).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.display_name").value("Care Agent"));
        anonymousMvc.perform(get("/api/customers").session(session)).andExpect(status().isOk());

        Map<String, Object> event = auditEvents().getLast();
        assertEquals("LOGIN", event.get("action"));
        assertEquals("care_agent", event.get("actor_username"));
        assertEquals("CARE_AGENT", event.get("actor_role"));
    }

    @Test
    void signInReplacesAnExistingSession() throws Exception {
        MockHttpSession before = new MockHttpSession();
        MvcResult result = anonymousMvc.perform(post("/api/auth/login").session(before).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + STAFF_PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(before.isInvalid());
        assertNotNull(result.getRequest().getSession(false));
    }

    @Test
    void wrongPasswordsAndUnknownUsersGetTheSameAnswer() throws Exception {
        login("care_agent", "wrong-password-1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(INVALID));
        login("nobody", STAFF_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(INVALID));
        assertEquals(1, failedLogins("care_agent"));
        List<Map<String, Object>> events = auditEvents();
        assertEquals("LOGIN_FAILED", events.get(0).get("action"));
        assertEquals("FAILED", events.get(0).get("outcome"));
        assertEquals("nobody", events.get(1).get("actor_username"));
    }

    @Test
    void fiveWrongPasswordsLockTheAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("care_agent", "wrong-password-" + i).andExpect(status().isUnauthorized());
        }
        login("care_agent", STAFF_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("This account is locked")));
        assertTrue(auditEvents().stream().anyMatch(e -> "ACCOUNT_LOCKED".equals(e.get("action"))));
    }

    @Test
    void aSuccessfulSignInResetsTheFailureCount() throws Exception {
        login("care_agent", "wrong-password-1").andExpect(status().isUnauthorized());
        login("care_agent", STAFF_PASSWORD).andExpect(status().isOk());
        assertEquals(0, failedLogins("care_agent"));
    }

    @Test
    void disabledAccountsCannotSignIn() throws Exception {
        jdbc.update("UPDATE staff_users SET is_active = 0 WHERE username = 'care_agent'");
        login("care_agent", STAFF_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("disabled")));
    }

    @Test
    void signOutEndsTheSession() throws Exception {
        MockHttpSession session = signIn(StaffRole.CARE_AGENT);
        anonymousMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        assertEquals("LOGOUT", auditEvents().getLast().get("action"));
    }

    @Test
    void stateChangingRequestsNeedTheCsrfToken() throws Exception {
        anonymousMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + STAFF_PASSWORD + "\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.title").value("Invalid CSRF token"));
        MockHttpSession session = signIn(StaffRole.ADMIN);
        anonymousMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void theCsrfCookieTokenIsAcceptedAsAHeader() throws Exception {
        MvcResult csrf = anonymousMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN")).andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn();
        Cookie token = csrf.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(token);
        anonymousMvc.perform(post("/api/auth/login").cookie(token).header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + STAFF_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        anonymousMvc.perform(post("/api/auth/login").cookie(token).header("X-XSRF-TOKEN", "forged")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + STAFF_PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanChangeTheirOwnPassword() throws Exception {
        MockHttpSession session = signIn(StaffRole.CARE_AGENT);
        String newPassword = "Brand-New-Pass-77";
        anonymousMvc.perform(post("/api/auth/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"nope\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.currentPassword").value("is incorrect"));
        anonymousMvc.perform(post("/api/auth/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + STAFF_PASSWORD + "\",\"newPassword\":\"short1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.newPassword").exists());
        anonymousMvc.perform(post("/api/auth/password").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + STAFF_PASSWORD + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isNoContent());

        anonymousMvc.perform(get(ME).session(session)).andExpect(status().isOk());
        login("care_agent", STAFF_PASSWORD).andExpect(status().isUnauthorized());
        login("care_agent", newPassword).andExpect(status().isOk());
        assertTrue(auditEvents().stream().anyMatch(e -> "PASSWORD_CHANGED".equals(e.get("action"))));
    }

    @Test
    void demoAccountsAreHiddenWithoutDemoData() throws Exception {
        anonymousMvc.perform(get("/api/auth/demo-accounts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isEmpty()).andExpect(jsonPath("$.password").doesNotExist());
    }

    private int failedLogins(String username) {
        Integer count = jdbc.queryForObject("SELECT failed_logins FROM staff_users WHERE username = ?",
                Integer.class, username);
        return count == null ? -1 : count;
    }
}
