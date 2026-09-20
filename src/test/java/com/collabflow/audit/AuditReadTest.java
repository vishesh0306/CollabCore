package com.collabflow.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** Reading the log: a task's history, a team's and a project's activity, and the admin's view. */
class AuditReadTest extends ApiTest {

    private TestUser manager;
    private TestUser member;
    private TestUser outsider;
    private UUID teamId;
    private TestProject project;
    private String taskKey;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        outsider = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        project = createProject(manager, teamId);

        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\", \"assigneeIds\": [\"" + member.id() + "\"]}"))
                .andReturn().getResponse().getContentAsString();
        taskKey = JsonPath.read(body, "$.key");
    }

    @Test
    void aTaskShowsItsWholeHistoryNewestFirst() throws Exception {
        mockMvc.perform(put("/api/v1/tasks/{key}/status", taskKey).header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"IN_PROGRESS\"}"));

        mockMvc.perform(get("/api/v1/tasks/{key}/history", taskKey).header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[0].actor.name").value("Test User"))
                .andExpect(jsonPath("$[0].changes[0].field").value("status"))
                .andExpect(jsonPath("$[0].changes[0].oldValue").value("TO_DO"))
                .andExpect(jsonPath("$[0].changes[0].newValue").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[2].action").value("CREATED"));
    }

    @Test
    void aTaskKeepsItsHistoryAfterItIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/{key}", taskKey).header("Authorization", manager.token()))
                .andExpect(status().isNoContent());

        // The task itself is gone from every list and from GET /tasks/{key}...
        mockMvc.perform(get("/api/v1/tasks/{key}", taskKey).header("Authorization", member.token()))
                .andExpect(status().isNotFound());
        // ...but what was done to it is still on the record, deletion included.
        mockMvc.perform(get("/api/v1/tasks/{key}/history", taskKey).header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("DELETED"))
                .andExpect(jsonPath("$[*].action", hasItem("CREATED")));
    }

    @Test
    void aTasksHistoryIncludesItsComments() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/{key}/comments", taskKey).header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"On it\"}"));

        mockMvc.perform(get("/api/v1/tasks/{key}/history", taskKey).header("Authorization", member.token()))
                .andExpect(jsonPath("$[0].entityType").value("COMMENT"))
                .andExpect(jsonPath("$[0].action").value("CREATED"));
    }

    @Test
    void anOutsiderCannotReadATasksHistory() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{key}/history", taskKey).header("Authorization", outsider.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aTeamsActivityCoversEverythingInIt() throws Exception {
        teamActivity(member)
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.items[0].entityLabel").value(taskKey));
    }

    @Test
    void anOutsiderCannotReadATeamsActivity() throws Exception {
        mockMvc.perform(get("/api/v1/teams/{id}/activity", teamId).header("Authorization", outsider.token()))
                .andExpect(status().isNotFound()); // not 403: outsiders can't tell the team exists
    }

    @Test
    void aProjectsActivityIsItsOwnChangesPlusItsTasks() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/{key}/comments", taskKey).header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"On it\"}"));

        mockMvc.perform(get("/api/v1/projects/{id}/activity", project.id())
                        .header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].entityType").value("COMMENT"))
                // nothing from the team itself leaks in: memberships belong to no project
                .andExpect(jsonPath("$.items[*].entityType",
                        everyItem(anyOf(is("COMMENT"), is("TASK"), is("PROJECT")))));
    }

    @Test
    void pagingWalksBackwardsWithACursorInsteadOfPageNumbers() throws Exception {
        String firstPage = mockMvc.perform(get("/api/v1/teams/{id}/activity", teamId)
                        .param("size", "2").header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        Integer firstId = JsonPath.read(firstPage, "$.items[0].id");
        Integer nextBefore = JsonPath.read(firstPage, "$.nextBefore");

        String secondPage = mockMvc.perform(get("/api/v1/teams/{id}/activity", teamId)
                        .param("size", "2").param("before", String.valueOf(nextBefore))
                        .header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer thirdId = JsonPath.read(secondPage, "$.items[0].id");
        assertThat(thirdId).isLessThan(nextBefore).isLessThan(firstId);
    }

    @Test
    void theLastPageSaysThereIsNoMore() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{id}/activity", project.id())
                        .param("size", "200").header("Authorization", member.token()))
                .andExpect(jsonPath("$.nextBefore").doesNotExist());
    }

    @Test
    void onlyTheAdminReadsTheWholeCompanysLog() throws Exception {
        mockMvc.perform(get("/api/v1/audit").header("Authorization", manager.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/audit").param("teamId", teamId.toString())
                        .param("entityType", "TASK").param("action", "CREATED")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].entityLabel").value(taskKey));
    }

    @Test
    void aSizeOutsideTheAllowedRangeIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/teams/{id}/activity", teamId).param("size", "500")
                        .header("Authorization", member.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("size must be between 1 and 200"));
    }

    private ResultActions teamActivity(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/teams/{id}/activity", teamId)
                        .header("Authorization", user.token()))
                .andExpect(status().isOk());
    }
}
