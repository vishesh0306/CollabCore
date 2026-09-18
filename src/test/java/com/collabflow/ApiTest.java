package com.collabflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import com.collabflow.identity.AdminProperties;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base class for tests that call the API: starts the whole app with a test PostgreSQL,
 * and offers helpers to register users and log them in.
 *
 * <p>All tests share one database, so each test creates its own users with unique emails.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private AdminProperties adminProperties;

    protected ResultActions register(String name, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "email": "%s", "password": "%s"}
                        """.formatted(name, email, password)));
    }

    protected ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    /** Registers a new user and returns the value for their Authorization header. */
    protected String registerAndLogin(String email, String password) throws Exception {
        register("Test User", email, password);
        return tokenFor(email, password);
    }

    /** A freshly registered user with a unique email, already logged in. */
    protected TestUser newUser() throws Exception {
        String email = uniqueEmail();
        String token = registerAndLogin(email, "test-password");
        String me = mockMvc.perform(get("/api/v1/me").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        return new TestUser(UUID.fromString(JsonPath.read(me, "$.id")), email, token);
    }

    /** The Authorization header value for the admin created at startup. */
    protected String adminToken() throws Exception {
        return tokenFor(adminProperties.email(), adminProperties.password());
    }

    /** Creates a team (as the admin) with this user as its manager, and returns the team's id. */
    protected UUID createTeam(TestUser manager) throws Exception {
        String body = mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Team %s", "managerEmail": "%s"}
                                """.formatted(UUID.randomUUID(), manager.email())))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    /** Adds a user to a team (as the admin) with the role "MANAGER" or "MEMBER". */
    protected void addToTeam(UUID teamId, TestUser user, String role) throws Exception {
        mockMvc.perform(post("/api/v1/teams/{teamId}/members", teamId)
                .header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "role": "%s"}
                        """.formatted(user.email(), role)));
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String tokenFor(String email, String password) throws Exception {
        String body = login(email, password).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    protected record TestUser(UUID id, String email, String token) {
    }
}
