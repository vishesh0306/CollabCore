package com.collabflow.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.collabflow.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapTest extends ApiTest {

    @Autowired
    private AdminBootstrap adminBootstrap;

    @Autowired
    private AdminProperties adminProperties;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminIsCreatedAtStartupAndCanLogIn() throws Exception {
        String body = login(adminProperties.email(), adminProperties.password())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/v1/me").header("Authorization", token))
                .andExpect(jsonPath("$.admin").value(true));
    }

    @Test
    void startingAgainDoesNotCreateASecondAdmin() {
        adminBootstrap.run(null);
        adminBootstrap.run(null);

        assertThat(userRepository.findAll().stream().filter(User::isAdmin)).hasSize(1);
    }

    @Test
    void theDatabaseRejectsASecondAdmin() {
        User secondAdmin = new User("Second admin", uniqueEmail(), "not-a-real-hash", true);

        assertThatThrownBy(() -> userRepository.saveAndFlush(secondAdmin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesToStartWithoutAdminSettingsWhenNoAdminExists() {
        UserRepository emptyDatabase = mock(UserRepository.class);
        when(emptyDatabase.existsByAdminTrue()).thenReturn(false);
        AdminBootstrap bootstrap = new AdminBootstrap(
                emptyDatabase, mock(PasswordEncoder.class), new AdminProperties(null, null, "Admin"));

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("collabflow.admin.email");
    }
}
