package com.collabflow.task;

import static org.hamcrest.Matchers.containsInAnyOrder;
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

class TaskListTest extends ApiTest {

    private TestUser manager;
    private TestUser member;
    private UUID teamId;
    private TestProject payments;
    private TestProject search;
    private String pay1;
    private String pay2;
    private String srch1;

    /**
     * A team with two projects and three tasks:
     * PAY-1 assigned to the member, due 10 Oct, IN_PROGRESS;
     * PAY-2 unassigned, due 20 Oct, TO_DO;
     * SRCH-1 assigned to the member, due 30 Oct, TO_DO.
     */
    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        payments = createProject(manager, teamId);
        search = createProject(manager, teamId);

        pay1 = createTask(payments, "Refund API", member.id(), "2026-10-10");
        pay2 = createTask(payments, "Payout API", null, "2026-10-20");
        srch1 = createTask(search, "Ranking", member.id(), "2026-10-30");
        mockMvc.perform(put("/api/v1/tasks/{key}/status", pay1)
                .header("Authorization", manager.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"IN_PROGRESS\"}"));
    }

    @Test
    void listsTheTeamsTasksNewestFirstOnePageAtATime() throws Exception {
        list("?size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].key").value(srch1))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        list("?size=2&page=1")
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].key").value(pay1));
    }

    @Test
    void filtersCanBeCombined() throws Exception {
        list("?projectId=" + payments.id())
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(pay1, pay2)));
        list("?status=TO_DO")
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(pay2, srch1)));
        list("?assigneeId=" + member.id())
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(pay1, srch1)));
        list("?expectedAfter=2026-10-15&expectedBefore=2026-10-25")
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(pay2)));
        list("?projectId=" + payments.id() + "&assigneeId=" + member.id() + "&status=IN_PROGRESS")
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(pay1)));
    }

    @Test
    void canBeSortedByAllowedFieldsOnly() throws Exception {
        list("?sort=expectedDate,asc")
                .andExpect(jsonPath("$.items[0].key").value(pay1))
                .andExpect(jsonPath("$.items[2].key").value(srch1));
        list("?sort=passwordHash")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletedTasksAreLeftOut() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/{key}", pay2).header("Authorization", manager.token()));

        list("").andExpect(jsonPath("$.items[*].key", containsInAnyOrder(pay1, srch1)));
    }

    @Test
    void outsidersGet404() throws Exception {
        mockMvc.perform(get("/api/v1/teams/{teamId}/tasks", teamId).header("Authorization", newUser().token()))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private ResultActions list(String query) throws Exception {
        return mockMvc.perform(get("/api/v1/teams/" + teamId + "/tasks" + query)
                .header("Authorization", member.token()));
    }

    private String createTask(TestProject project, String title, UUID assigneeId, String expectedDate)
            throws Exception {
        String assignees = assigneeId == null ? "" : "\"" + assigneeId + "\"";
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "expectedDate": "%s", "assigneeIds": [%s]}
                                """.formatted(title, expectedDate, assignees)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }
}
