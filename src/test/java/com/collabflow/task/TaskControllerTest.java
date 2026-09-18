package com.collabflow.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.collabflow.ApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TaskControllerTest extends ApiTest {

    private TestUser manager;
    private TestUser member;
    private TestUser otherMember;
    private UUID teamId;
    private TestProject project;

    /** Every test starts with a team (a manager and two members) and one project. */
    @BeforeEach
    void setUpTeamAndProject() throws Exception {
        manager = newUser();
        member = newUser();
        otherMember = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        addToTeam(teamId, otherMember, "MEMBER");
        project = createProject(manager, teamId);
    }

    @Test
    void aMemberCreatesTasksWithSequentialKeys() throws Exception {
        createTask(member.token(), "Refund API", List.of(otherMember.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(project.code() + "-1"))
                .andExpect(jsonPath("$.status").value("TO_DO"))
                .andExpect(jsonPath("$.createdBy.id").value(member.id().toString()))
                .andExpect(jsonPath("$.assignees[0].id").value(otherMember.id().toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        createTask(member.token(), "Payout API", List.of())
                .andExpect(jsonPath("$.key").value(project.code() + "-2"));
    }

    @Test
    void assigneesMustBeMembersOfTheTeam() throws Exception {
        TestUser outsider = newUser();

        createTask(member.token(), "Refund API", List.of(outsider.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Assignees must be members of the team"));
    }

    @Test
    void outsidersCanNeitherCreateNorSeeTasks() throws Exception {
        createTask(member.token(), "Refund API", List.of());
        TestUser outsider = newUser();

        createTask(outsider.token(), "Sneaky", List.of())
                .andExpect(status().isNotFound());
        getTask(outsider.token(), project.code() + "-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task not found"));
    }

    @Test
    void tasksAreFoundByKeyInAnyLetterCase() throws Exception {
        createTask(member.token(), "Refund API", List.of());

        getTask(otherMember.token(), project.code().toLowerCase() + "-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Refund API"));
        getTask(otherMember.token(), "not-a-key").andExpect(status().isNotFound());
        getTask(otherMember.token(), project.code() + "-999").andExpect(status().isNotFound());
    }

    @Test
    void noNewTasksInACompletedProject() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{id}/complete", project.id()).header("Authorization", manager.token()));

        createTask(member.token(), "Too late", List.of())
                .andExpect(status().isConflict());
    }

    // --- helpers ---

    private ResultActions createTask(String token, String title, List<UUID> assigneeIds) throws Exception {
        String ids = assigneeIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
        return mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", project.id())
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "%s", "description": "Details", "expectedDate": "2026-10-15", "assigneeIds": [%s]}
                        """.formatted(title, ids)));
    }

    private ResultActions getTask(String token, String key) throws Exception {
        return mockMvc.perform(get("/api/v1/tasks/{key}", key).header("Authorization", token));
    }
}
