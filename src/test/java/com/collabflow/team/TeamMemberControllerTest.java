package com.collabflow.team;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.collabflow.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TeamMemberControllerTest extends ApiTest {

    @Test
    void managerAddsAMemberWhoThenSeesTheTeam() throws Exception {
        TestUser manager = newUser();
        TestUser member = newUser();
        UUID teamId = createTeam(manager);

        addMember(manager.token(), teamId, member.email(), "MEMBER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(member.id().toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(get("/api/v1/teams").header("Authorization", member.token()))
                .andExpect(jsonPath("$[0].id").value(teamId.toString()))
                .andExpect(jsonPath("$[0].myRole").value("MEMBER"));
        mockMvc.perform(get("/api/v1/teams/{id}", teamId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].role").value("MANAGER")); // managers are listed first
    }

    @Test
    void membersCannotManagePeople() throws Exception {
        TestUser member = newUser();
        UUID teamId = createTeam(newUser());
        addToTeam(teamId, member, "MEMBER");

        addMember(member.token(), teamId, newUser().email(), "MEMBER")
                .andExpect(status().isForbidden());
    }

    @Test
    void outsidersGet404() throws Exception {
        UUID teamId = createTeam(newUser());
        TestUser outsider = newUser();

        addMember(outsider.token(), teamId, outsider.email(), "MANAGER")
                .andExpect(status().isNotFound());
    }

    @Test
    void theAdminManagesAnyTeamWithoutBeingInIt() throws Exception {
        UUID teamId = createTeam(newUser());

        addMember(adminToken(), teamId, newUser().email(), "MEMBER")
                .andExpect(status().isCreated());
    }

    @Test
    void addingSomeoneTwiceIsRejected() throws Exception {
        TestUser manager = newUser();
        TestUser member = newUser();
        UUID teamId = createTeam(manager);
        addMember(manager.token(), teamId, member.email(), "MEMBER").andExpect(status().isCreated());

        addMember(manager.token(), teamId, member.email(), "MANAGER")
                .andExpect(status().isConflict());
    }

    @Test
    void onlyRegisteredUsersCanBeAdded() throws Exception {
        TestUser manager = newUser();
        UUID teamId = createTeam(manager);

        addMember(manager.token(), teamId, uniqueEmail(), "MEMBER")
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPromotedMemberCanManageTheTeam() throws Exception {
        TestUser manager = newUser();
        TestUser member = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");

        changeRole(manager.token(), teamId, member.id(), "MANAGER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        addMember(member.token(), teamId, newUser().email(), "MEMBER")
                .andExpect(status().isCreated());
    }

    @Test
    void theLastManagerCannotBeDemotedOrRemoved() throws Exception {
        TestUser manager = newUser();
        UUID teamId = createTeam(manager);

        changeRole(manager.token(), teamId, manager.id(), "MEMBER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A team must keep at least one manager"));
        removeMember(manager.token(), teamId, manager.id())
                .andExpect(status().isConflict());
    }

    @Test
    void aManagerCanStepDownOnceThereIsAnotherManager() throws Exception {
        TestUser manager = newUser();
        TestUser secondManager = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, secondManager, "MANAGER");

        changeRole(manager.token(), teamId, manager.id(), "MEMBER")
                .andExpect(status().isOk());
    }

    @Test
    void removedMembersLoseAccess() throws Exception {
        TestUser manager = newUser();
        TestUser member = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");

        removeMember(manager.token(), teamId, member.id()).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/teams/{id}", teamId).header("Authorization", member.token()))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private ResultActions addMember(String token, UUID teamId, String email, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/teams/{teamId}/members", teamId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "role": "%s"}
                        """.formatted(email, role)));
    }

    private ResultActions changeRole(String token, UUID teamId, UUID userId, String role) throws Exception {
        return mockMvc.perform(put("/api/v1/teams/{teamId}/members/{userId}", teamId, userId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role": "%s"}
                        """.formatted(role)));
    }

    private ResultActions removeMember(String token, UUID teamId, UUID userId) throws Exception {
        return mockMvc.perform(delete("/api/v1/teams/{teamId}/members/{userId}", teamId, userId)
                .header("Authorization", token));
    }
}
