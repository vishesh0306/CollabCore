package com.collabflow.audit;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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

/** SPR-6's timeline: the sprint and its tasks, read back out of the audit log. */
class SprintTimelineTest extends ApiTest {

    private TestUser manager;
    private TestUser member;
    private UUID teamId;
    private TestProject project;
    private UUID sprintId;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        project = createProject(manager, teamId);
        sprintId = createSprint(manager, teamId);
    }

    @Test
    void theTimelineCoversTheSprintAndTheTasksTaggedIntoIt() throws Exception {
        String inSprint = createTask("Refund API");
        String elsewhere = createTask("Nothing to do with the sprint");
        tagIntoSprint(inSprint);
        mockMvc.perform(post("/api/v1/sprints/{id}/start", sprintId).header("Authorization", manager.token()));
        mockMvc.perform(put("/api/v1/tasks/{key}/status", inSprint).header("Authorization", manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"IN_PROGRESS\"}"));
        mockMvc.perform(post("/api/v1/tasks/{key}/comments", inSprint).header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"On it\"}"));

        mockMvc.perform(get("/api/v1/sprints/{id}/timeline", sprintId).header("Authorization", member.token()))
                .andExpect(status().isOk())
                // the sprint's own entries
                .andExpect(jsonPath("$.items[*].action", hasItem("STARTED")))
                // its task's entries, comments included
                .andExpect(jsonPath("$.items[*].action", hasItem("STATUS_CHANGED")))
                .andExpect(jsonPath("$.items[*].entityType", hasItem("COMMENT")))
                // and nothing about tasks that were never tagged into it
                .andExpect(jsonPath("$.items[*].entityLabel", everyItem(not(is(elsewhere)))));
    }

    @Test
    void aSprintWithNoTasksStillShowsItsOwnHistory() throws Exception {
        mockMvc.perform(get("/api/v1/sprints/{id}/timeline", sprintId).header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].action").value("CREATED"))
                .andExpect(jsonPath("$.items[0].entityType").value("SPRINT"));
    }

    @Test
    void theTimelineIsPagedWithACursor() throws Exception {
        for (int i = 0; i < 3; i++) {
            tagIntoSprint(createTask("Task " + i));
        }

        String first = mockMvc.perform(get("/api/v1/sprints/{id}/timeline", sprintId)
                        .param("size", "2").header("Authorization", member.token()))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        Integer cursor = JsonPath.read(first, "$.nextBefore");

        mockMvc.perform(get("/api/v1/sprints/{id}/timeline", sprintId)
                        .param("size", "2").param("before", String.valueOf(cursor))
                        .header("Authorization", member.token()))
                .andExpect(jsonPath("$.items[0].id", is(org.hamcrest.Matchers.lessThan(cursor))));
    }

    @Test
    void anOutsiderCannotReadASprintsTimeline() throws Exception {
        TestUser outsider = newUser();

        mockMvc.perform(get("/api/v1/sprints/{id}/timeline", sprintId).header("Authorization", outsider.token()))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private String createTask(String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private void tagIntoSprint(String key) throws Exception {
        mockMvc.perform(post("/api/v1/tasks/{key}/sprints/{sprintId}", key, sprintId)
                .header("Authorization", manager.token())).andExpect(status().isOk());
    }
}
