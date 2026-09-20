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

    /** Creates a project in the team (as its manager) with a unique code; returns its id and code. */
    protected TestProject createProject(TestUser manager, UUID teamId) throws Exception {
        String code = uniqueProjectCode();
        String body = mockMvc.perform(post("/api/v1/teams/{teamId}/projects", teamId)
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "Project %s", "leadUserId": "%s"}
                                """.formatted(code, code, manager.id())))
                .andReturn().getResponse().getContentAsString();
        return new TestProject(UUID.fromString(JsonPath.read(body, "$.id")), code);
    }

    /** Creates a planned sprint in the team (as its manager) and returns its id. */
    protected UUID createSprint(TestUser manager, UUID teamId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/teams/{teamId}/sprints", teamId)
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Sprint", "target": "Ship it", "startDate": "2026-10-01", "endDate": "2026-10-15"}
                                """))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    /**
     * Marks everything these users have been notified about as read, so a test can start from
     * a clean unread count. Setting a team up already notifies everyone who joins it.
     */
    protected void clearNotifications(TestUser... users) throws Exception {
        for (TestUser user : users) {
            mockMvc.perform(post("/api/v1/notifications/read-all").header("Authorization", user.token()));
        }
    }

    /** Project codes are unique across all tests in the shared database, e.g. "P3F9A1C0". */
    protected static String uniqueProjectCode() {
        return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
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

    protected record TestProject(UUID id, String code) {
    }
}
