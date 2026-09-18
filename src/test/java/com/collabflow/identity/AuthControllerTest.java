package com.collabflow.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.collabflow.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthControllerTest extends ApiTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerCreatesUserWithHashedPassword() throws Exception {
        String email = uniqueEmail();

        register("Priya", email, "correct-horse-battery")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain("correct-horse-battery");
    }

    @Test
    void emailIsStoredLowercase() throws Exception {
        String email = uniqueEmail();

        register("Priya", "  " + email.toUpperCase() + " ", "correct-horse-battery")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() throws Exception {
        String email = uniqueEmail();
        register("Priya", email, "correct-horse-battery").andExpect(status().isCreated());

        register("Someone else", email.toUpperCase(), "another-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This email is already registered"));
    }

    @Test
    void invalidSignUpListsTheBadFields() throws Exception {
        register("", "not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginReturnsAToken() throws Exception {
        String email = uniqueEmail();
        register("Priya", email, "correct-horse-battery");

        login(email, "correct-horse-battery")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(24 * 60 * 60));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        String email = uniqueEmail();
        register("Priya", email, "correct-horse-battery");

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        login(uniqueEmail(), "correct-horse-battery")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }
}
