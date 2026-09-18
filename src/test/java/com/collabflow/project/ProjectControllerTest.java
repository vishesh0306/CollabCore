package com.collabflow.project;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.collabflow.ApiTest;
import com.collabflow.identity.User;
import com.collabflow.identity.UserRepository;
import com.collabflow.team.Team;
import com.collabflow.team.TeamRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ProjectControllerTest extends ApiTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    private TestUser manager;
    private TestUser member;
    private UUID teamId;

    /** Every test starts with a team that has one manager and one member. */
    @BeforeEach
    void setUpTeam() throws Exception {
        manager = newUser();
        member = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
    }

    @Test
    void managerCreatesAProject() throws Exception {
        String code = uniqueProjectCode();

        createProject(manager.token(), teamId, code.toLowerCase(), manager.id())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code)) // stored in capitals
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.teamId").value(teamId.toString()))
                .andExpect(jsonPath("$.leadUserId").value(manager.id().toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void membersCannotCreateProjects() throws Exception {
        createProject(member.token(), teamId, uniqueProjectCode(), manager.id())
                .andExpect(status().isForbidden());
    }

    @Test
    void codesAreUniqueAcrossTheWholeCompany() throws Exception {
        String code = uniqueProjectCode();
        createProject(manager.token(), teamId, code, manager.id()).andExpect(status().isCreated());

        TestUser otherManager = newUser();
        UUID otherTeamId = createTeam(otherManager);
        createProject(otherManager.token(), otherTeamId, code, otherManager.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The project code " + code + " is already used"));
    }

    @Test
    void codesMustHaveTheRightFormat() throws Exception {
        for (String badCode : new String[] {"P", "1PAY", "PAY-1", "WAYTOOLONGCODE"}) {
            createProject(manager.token(), teamId, badCode, manager.id())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.code").exists());
        }
    }

    @Test
    void theLeadMustBeAManagerOfTheTeam() throws Exception {
        createProject(manager.token(), teamId, uniqueProjectCode(), member.id())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The lead must be one of the team's managers"));
        createProject(manager.token(), teamId, uniqueProjectCode(), newUser().id())
                .andExpect(status().isBadRequest());
    }

    @Test
    void everyoneInTheTeamSeesItsProjects() throws Exception {
        String projectId = createProjectAndGetId();

        mockMvc.perform(get("/api/v1/teams/{teamId}/projects", teamId).header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(projectId))
                .andExpect(jsonPath("$[0].leadName").value("Test User"));
        mockMvc.perform(get("/api/v1/projects/{id}", projectId).header("Authorization", member.token()))
                .andExpect(status().isOk());
    }

    @Test
    void outsidersGet404() throws Exception {
        String projectId = createProjectAndGetId();
        TestUser outsider = newUser();

        mockMvc.perform(get("/api/v1/projects/{id}", projectId).header("Authorization", outsider.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project not found"));
        mockMvc.perform(get("/api/v1/teams/{teamId}/projects", teamId).header("Authorization", outsider.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerEditsAProjectButNotItsCode() throws Exception {
        String projectId = createProjectAndGetId();

        updateProject(manager.token(), projectId, "Payments v2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Payments v2"));
        updateProject(member.token(), projectId, "Hijacked")
                .andExpect(status().isForbidden());
    }

    @Test
    void aCompletedProjectIsReadOnlyUntilReopened() throws Exception {
        String projectId = createProjectAndGetId();

        postAction(manager.token(), projectId, "complete")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        updateProject(manager.token(), projectId, "Late change")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This project is completed. Reopen it to make changes."));

        postAction(manager.token(), projectId, "reopen")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        updateProject(manager.token(), projectId, "Late change").andExpect(status().isOk());
    }

    @Test
    void completingOrReopeningTwiceIsRejected() throws Exception {
        String projectId = createProjectAndGetId();

        postAction(manager.token(), projectId, "reopen").andExpect(status().isConflict());
        postAction(manager.token(), projectId, "complete").andExpect(status().isOk());
        postAction(manager.token(), projectId, "complete").andExpect(status().isConflict());
    }

    @Test
    void membersCannotCompleteProjects() throws Exception {
        String projectId = createProjectAndGetId();

        postAction(member.token(), projectId, "complete").andExpect(status().isForbidden());
    }

    @Test
    void theListCanBeFilteredByStatus() throws Exception {
        String activeId = createProjectAndGetId();
        String completedId = createProjectAndGetId();
        postAction(manager.token(), completedId, "complete");

        mockMvc.perform(get("/api/v1/teams/{teamId}/projects?status=COMPLETED", teamId)
                        .header("Authorization", member.token()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(completedId));
        mockMvc.perform(get("/api/v1/teams/{teamId}/projects?status=ACTIVE", teamId)
                        .header("Authorization", member.token()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activeId));
    }

    @Test
    void theDatabaseRejectsABadCodeEvenWhenTheApiIsSkipped() {
        Team team = teamRepository.findById(teamId).orElseThrow();
        User lead = userRepository.findById(manager.id()).orElseThrow();
        Project badProject = new Project(team, "pay-1", "Bad code", null, lead);

        assertThatThrownBy(() -> projectRepository.saveAndFlush(badProject))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- helpers ---

    private ResultActions postAction(String token, String projectId, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/projects/{id}/" + action, projectId).header("Authorization", token));
    }

    private ResultActions createProject(String token, UUID teamId, String code, UUID leadUserId) throws Exception {
        return mockMvc.perform(post("/api/v1/teams/{teamId}/projects", teamId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code": "%s", "name": "Payments", "description": "Refunds and payouts", "leadUserId": "%s"}
                        """.formatted(code, leadUserId)));
    }

    private String createProjectAndGetId() throws Exception {
        String body = createProject(manager.token(), teamId, uniqueProjectCode(), manager.id())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions updateProject(String token, String projectId, String name) throws Exception {
        return mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "description": "Updated", "leadUserId": "%s"}
                        """.formatted(name, manager.id())));
    }
}
