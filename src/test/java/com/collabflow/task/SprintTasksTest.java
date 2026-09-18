package com.collabflow.task;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.collabflow.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class SprintTasksTest extends ApiTest {

    private TestUser manager;
    private TestUser member;
    private TestUser otherMember;
    private UUID teamId;
    private TestProject payments;
    private TestProject search;
    private UUID sprintId;

    /** A team with two projects and one planned sprint. */
    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        otherMember = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        addToTeam(teamId, otherMember, "MEMBER");
        payments = createProject(manager, teamId);
        search = createProject(manager, teamId);
        sprintId = createSprint(manager, teamId);
    }

    @Test
    void aMemberMovesTheirTaskIntoASprintAndBackToTheBacklog() throws Exception {
        String key = createTask(member, payments);

        moveToSprint(member, key, sprintId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value(sprintId.toString()));
        backlog(payments).andExpect(jsonPath("$.totalItems").value(0));

        moveToSprint(member, key, null)
                .andExpect(jsonPath("$.sprintId").doesNotExist());
        backlog(payments).andExpect(jsonPath("$.items[0].key").value(key));
    }

    @Test
    void membersCannotMoveOtherPeoplesTasks() throws Exception {
        String key = createTask(member, payments);

        moveToSprint(otherMember, key, sprintId).andExpect(status().isForbidden());
    }

    @Test
    void tasksOnlyGoIntoOpenSprintsOfTheirOwnTeam() throws Exception {
        String key = createTask(member, payments);
        TestUser otherManager = newUser();
        UUID otherTeamsSprint = createSprint(otherManager, createTeam(otherManager));

        moveToSprint(member, key, otherTeamsSprint)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The sprint must belong to the task's team"));

        sprintAction("start");
        sprintAction("complete");
        moveToSprint(member, key, sprintId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tasks can only be added to a planned or active sprint"));
    }

    @Test
    void theSprintViewGroupsTasksByProjectWithProgress() throws Exception {
        String pay1 = createTask(member, payments);
        String pay2 = createTask(member, payments);
        String srch1 = createTask(member, search);
        for (String key : new String[] {pay1, pay2, srch1}) {
            moveToSprint(member, key, sprintId);
        }
        changeStatus(pay1, "DONE");

        mockMvc.perform(get("/api/v1/sprints/{id}/tasks", sprintId).header("Authorization", otherMember.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(1))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.projects[?(@.code == '" + payments.code() + "')].done").value(1))
                .andExpect(jsonPath("$.projects[?(@.code == '" + payments.code() + "')].total").value(2))
                .andExpect(jsonPath("$.projects[?(@.code == '" + search.code() + "')].total").value(1));
    }

    @Test
    void completingASprintSendsUnfinishedTasksBackToTheBacklog() throws Exception {
        String done = createTask(member, payments);
        String unfinished = createTask(member, payments);
        moveToSprint(member, done, sprintId);
        moveToSprint(member, unfinished, sprintId);
        changeStatus(done, "DONE");

        sprintAction("start");
        sprintAction("complete").andExpect(status().isOk());

        backlog(payments).andExpect(jsonPath("$.items[*].key", containsInAnyOrder(unfinished)));
        mockMvc.perform(get("/api/v1/sprints/{id}/tasks", sprintId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.projects[0].tasks[0].key").value(done));
        moveToSprint(member, done, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This task is part of a completed sprint and stays there"));
    }

    @Test
    void theTaskListCanBeFilteredBySprintOrBacklog() throws Exception {
        String inSprint = createTask(member, payments);
        String inBacklog = createTask(member, search);
        moveToSprint(member, inSprint, sprintId);

        mockMvc.perform(get("/api/v1/teams/" + teamId + "/tasks?sprintId=" + sprintId)
                        .header("Authorization", member.token()))
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(inSprint)));
        mockMvc.perform(get("/api/v1/teams/" + teamId + "/tasks?backlog=true")
                        .header("Authorization", member.token()))
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(inBacklog)));
    }

    // --- helpers ---

    private String createTask(TestUser creator, TestProject project) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", creator.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"A task\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private ResultActions moveToSprint(TestUser user, String key, UUID sprint) throws Exception {
        String sprintJson = sprint == null ? "null" : "\"" + sprint + "\"";
        return mockMvc.perform(put("/api/v1/tasks/{key}/sprint", key)
                .header("Authorization", user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sprintId\": " + sprintJson + "}"));
    }

    private void changeStatus(String key, String status) throws Exception {
        mockMvc.perform(put("/api/v1/tasks/{key}/status", key)
                .header("Authorization", manager.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"" + status + "\"}"));
    }

    private ResultActions sprintAction(String action) throws Exception {
        return mockMvc.perform(post("/api/v1/sprints/{id}/" + action, sprintId)
                .header("Authorization", manager.token()));
    }

    private ResultActions backlog(TestProject project) throws Exception {
        return mockMvc.perform(get("/api/v1/projects/{id}/backlog", project.id())
                .header("Authorization", member.token()));
    }
}
