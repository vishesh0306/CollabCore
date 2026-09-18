package com.collabflow.team;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.collabflow.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TeamControllerTest extends ApiTest {

    @Test
    void adminCreatesATeamWithItsFirstManager() throws Exception {
        TestUser manager = newUser();
        String name = uniqueTeamName();

        createTeam(adminToken(), name, manager.email())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.members[0].email").value(manager.email()))
                .andExpect(jsonPath("$.members[0].role").value("MANAGER"));
    }

    @Test
    void onlyTheAdminCanCreateTeams() throws Exception {
        TestUser user = newUser();

        createTeam(user.token(), uniqueTeamName(), user.email())
                .andExpect(status().isForbidden());
    }

    @Test
    void teamNamesAreUniqueIgnoringCase() throws Exception {
        String name = uniqueTeamName();
        createTeam(adminToken(), name, newUser().email()).andExpect(status().isCreated());

        createTeam(adminToken(), name.toUpperCase(), newUser().email())
                .andExpect(status().isConflict());
    }

    @Test
    void theAdminCannotBeATeamManager() throws Exception {
        createTeam(adminToken(), uniqueTeamName(), adminEmail())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The admin can't be added to a team"));
    }

    @Test
    void aNewUserSeesNoTeams() throws Exception {
        TestUser newcomer = newUser();

        mockMvc.perform(get("/api/v1/teams").header("Authorization", newcomer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void managerSeesTheTeamWithTheirRole() throws Exception {
        TestUser manager = newUser();
        String teamId = createTeamAndGetId(uniqueTeamName(), manager.email());

        mockMvc.perform(get("/api/v1/teams").header("Authorization", manager.token()))
                .andExpect(jsonPath("$[0].id").value(teamId))
                .andExpect(jsonPath("$[0].myRole").value("MANAGER"));
        mockMvc.perform(get("/api/v1/teams/{id}", teamId).header("Authorization", manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void outsidersGet404AsIfTheTeamDidNotExist() throws Exception {
        String teamId = createTeamAndGetId(uniqueTeamName(), newUser().email());
        TestUser outsider = newUser();

        mockMvc.perform(get("/api/v1/teams/{id}", teamId).header("Authorization", outsider.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Team not found"));
        mockMvc.perform(get("/api/v1/teams").header("Authorization", outsider.token()))
                .andExpect(jsonPath("$[*].id", not(hasItem(teamId))));
    }

    @Test
    void theAdminSeesEveryTeam() throws Exception {
        String teamId = createTeamAndGetId(uniqueTeamName(), newUser().email());

        mockMvc.perform(get("/api/v1/teams").header("Authorization", adminToken()))
                .andExpect(jsonPath("$[*].id", hasItem(teamId)));
        mockMvc.perform(get("/api/v1/teams/{id}", teamId).header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void onlyTheAdminCanRenameATeam() throws Exception {
        TestUser manager = newUser();
        String teamId = createTeamAndGetId(uniqueTeamName(), manager.email());
        String newName = uniqueTeamName();

        renameTeam(manager.token(), teamId, newName).andExpect(status().isForbidden());
        renameTeam(adminToken(), teamId, newName)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
    }

    // --- helpers ---

    private ResultActions createTeam(String token, String name, String managerEmail) throws Exception {
        return mockMvc.perform(post("/api/v1/teams")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "description": "A test team", "managerEmail": "%s"}
                        """.formatted(name, managerEmail)));
    }

    private String createTeamAndGetId(String name, String managerEmail) throws Exception {
        String body = createTeam(adminToken(), name, managerEmail).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions renameTeam(String token, String teamId, String name) throws Exception {
        return mockMvc.perform(put("/api/v1/teams/{id}", teamId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "description": "Renamed"}
                        """.formatted(name)));
    }

    private String adminEmail() throws Exception {
        String me = mockMvc.perform(get("/api/v1/me").header("Authorization", adminToken()))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(me, "$.email");
    }

    private static String uniqueTeamName() {
        return "Team " + UUID.randomUUID();
    }
}
