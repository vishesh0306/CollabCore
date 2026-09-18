package com.collabflow.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.collabflow.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AccountControllerTest extends ApiTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Test
    void meShowsTheLoggedInUser() throws Exception {
        String email = uniqueEmail();
        String token = registerAndLogin(email, PASSWORD);

        mockMvc.perform(get("/api/v1/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void changingPasswordReplacesTheOldOne() throws Exception {
        String email = uniqueEmail();
        String token = registerAndLogin(email, PASSWORD);

        changePassword(token, PASSWORD, "brand-new-password").andExpect(status().isNoContent());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "brand-new-password").andExpect(status().isOk());
    }

    @Test
    void changingPasswordNeedsTheCurrentPassword() throws Exception {
        String token = registerAndLogin(uniqueEmail(), PASSWORD);

        changePassword(token, "wrong-password", "brand-new-password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Current password is incorrect"));
    }

    @Test
    void changingEmailMovesTheLoginToTheNewEmail() throws Exception {
        String oldEmail = uniqueEmail();
        String newEmail = uniqueEmail();
        String token = registerAndLogin(oldEmail, PASSWORD);

        changeEmail(token, newEmail, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));

        login(oldEmail, PASSWORD).andExpect(status().isUnauthorized());
        login(newEmail, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void cannotTakeAnEmailThatIsAlreadyRegistered() throws Exception {
        String takenEmail = uniqueEmail();
        register("Other", takenEmail, PASSWORD);
        String token = registerAndLogin(uniqueEmail(), PASSWORD);

        changeEmail(token, takenEmail, PASSWORD).andExpect(status().isConflict());
    }

    @Test
    void changingEmailNeedsTheCurrentPassword() throws Exception {
        String token = registerAndLogin(uniqueEmail(), PASSWORD);

        changeEmail(token, uniqueEmail(), "wrong-password").andExpect(status().isBadRequest());
    }

    private ResultActions changePassword(String token, String currentPassword, String newPassword) throws Exception {
        return mockMvc.perform(put("/api/v1/me/password")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(currentPassword, newPassword)));
    }

    private ResultActions changeEmail(String token, String newEmail, String currentPassword) throws Exception {
        return mockMvc.perform(put("/api/v1/me/email")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newEmail": "%s", "currentPassword": "%s"}
                        """.formatted(newEmail, currentPassword)));
    }
}
