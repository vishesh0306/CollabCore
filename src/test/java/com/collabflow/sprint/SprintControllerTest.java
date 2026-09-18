package com.collabflow.sprint;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.collabflow.ApiTest;
import com.collabflow.team.Team;
import com.collabflow.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class SprintControllerTest extends ApiTest {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TeamRepository teamRepository;

    private TestUser manager;
    private TestUser member;
    private UUID teamId;

    @BeforeEach
    void setUpTeam() throws Exception {
        manager = newUser();
        member = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
    }

    @Test
    void aManagerPlansASprint() throws Exception {
        createSprint(manager.token(), "Sprint 5", "2026-10-01", "2026-10-15")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sprint 5"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.teamId").value(teamId.toString()));
    }

    @Test
    void membersSeeSprintsButCannotPlanThem() throws Exception {
        UUID sprintId = createSprint(manager, teamId);

        createSprint(member.token(), "Sprint X", "2026-10-01", "2026-10-15").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sprints/{id}", sprintId).header("Authorization", member.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/sprints/{id}", sprintId).header("Authorization", newUser().token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void theEndDateCannotBeBeforeTheStartDate() throws Exception {
        createSprint(manager.token(), "Backwards", "2026-10-15", "2026-10-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The end date can't be before the start date"));
    }

    @Test
    void onlyOneSprintPerTeamCanBeActive() throws Exception {
        UUID first = createSprint(manager, teamId);
        UUID second = createSprint(manager, teamId);

        action(first, "start").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        action(second, "start")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Another sprint is already active in this team. Complete it first."));

        action(first, "complete").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        action(second, "start").andExpect(status().isOk());
    }

    @Test
    void differentTeamsEachHaveTheirOwnActiveSprint() throws Exception {
        TestUser otherManager = newUser();
        UUID otherTeamId = createTeam(otherManager);
        action(createSprint(manager, teamId), "start").andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sprints/{id}/start", createSprint(otherManager, otherTeamId))
                        .header("Authorization", otherManager.token()))
                .andExpect(status().isOk());
    }

    @Test
    void sprintsOnlyMoveForward() throws Exception {
        UUID sprintId = createSprint(manager, teamId);

        action(sprintId, "complete").andExpect(status().isConflict()); // not started yet
        action(sprintId, "start");
        action(sprintId, "complete");
        action(sprintId, "start").andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/sprints/{id}", sprintId)
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renamed", "startDate": "2026-10-01", "endDate": "2026-10-15"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void theListCanBeFilteredByStatus() throws Exception {
        UUID planned = createSprint(manager, teamId);
        UUID active = createSprint(manager, teamId);
        action(active, "start");

        mockMvc.perform(get("/api/v1/teams/{teamId}/sprints?status=ACTIVE", teamId)
                        .header("Authorization", member.token()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(active.toString()));
        mockMvc.perform(get("/api/v1/teams/{teamId}/sprints", teamId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void theDatabaseRefusesASecondActiveSprintEvenWhenTheApiIsSkipped() {
        Team team = teamRepository.findById(teamId).orElseThrow();
        Sprint first = new Sprint(team, "One", null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
        first.start();
        sprintRepository.saveAndFlush(first);

        Sprint second = new Sprint(team, "Two", null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
        second.start();
        assertThatThrownBy(() -> sprintRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- helpers ---

    private ResultActions createSprint(String token, String name, String startDate, String endDate) throws Exception {
        return mockMvc.perform(post("/api/v1/teams/{teamId}/sprints", teamId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "target": "Launch refunds", "startDate": "%s", "endDate": "%s"}
                        """.formatted(name, startDate, endDate)));
    }

    private ResultActions action(UUID sprintId, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/sprints/{id}/" + action, sprintId)
                .header("Authorization", manager.token()));
    }
}
