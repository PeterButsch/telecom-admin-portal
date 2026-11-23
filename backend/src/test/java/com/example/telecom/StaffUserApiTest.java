package com.example.telecom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.telecom.security.StaffRole;
import org.junit.jupiter.api.Test;

class StaffUserApiTest extends IntegrationTestSupport {

    private static final String NEW_USER = """
            {"username":"wei.ling","displayName":"Wei Ling","email":"wei.ling@staff.test","role":"CARE_AGENT",
             "password":"Welcome-to-care-1"}""";

    @Test
    void adminsCreateAccountsThatCanSignIn() throws Exception {
        postJson("/api/staff-users", NEW_USER).andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("wei.ling")).andExpect(jsonPath("$.is_active").value(true))
                .andExpect(jsonPath("$.password_hash").doesNotExist());
        login("wei.ling", "Welcome-to-care-1").andExpect(status().isOk());
        postJson("/api/staff-users", NEW_USER).andExpect(status().isConflict());
    }

    @Test
    void newAccountsMustFollowTheRules() throws Exception {
        postJson("/api/staff-users", NEW_USER.replace("Welcome-to-care-1", "password"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
        postJson("/api/staff-users", NEW_USER.replace("Welcome-to-care-1", "wei.ling-2026-x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("must not contain the username"));
        postJson("/api/staff-users", NEW_USER.replace("\"wei.ling\"", "\"Wei Ling\""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.username").exists());
        postJson("/api/staff-users", NEW_USER.replace("CARE_AGENT", "SUPERUSER")).andExpect(status().isBadRequest());
    }

    @Test
    void adminsCannotLockThemselvesOut() throws Exception {
        long admin = staffId(StaffRole.ADMIN);
        patchJson("/api/staff-users/" + admin, "{\"role\":\"CARE_AGENT\"}").andExpect(status().isBadRequest());
        patchJson("/api/staff-users/" + admin, "{\"active\":false}").andExpect(status().isBadRequest());
        patchJson("/api/staff-users/" + admin, "{\"displayName\":\"Chief Admin\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.display_name").value("Chief Admin"));
    }

    @Test
    void unlockingClearsALockout() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("care_agent", "wrong-password-" + i).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/staff-users")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'care_agent')].is_locked").value(true));
        long agent = staffId(StaffRole.CARE_AGENT);
        postJson("/api/staff-users/" + agent + "/unlock", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.is_locked").value(false));
        login("care_agent", STAFF_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unknownAccountsAreNotFound() throws Exception {
        patchJson("/api/staff-users/9999", "{\"active\":false}").andExpect(status().isNotFound());
    }
}
