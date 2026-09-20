package com.collabflow;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * One full story, through the API only: a team with managers and members runs a sprint
 * across its projects, from planning to completion. It reads as a summary of how the
 * product behaves; the detailed rules are covered by each feature's own tests.
 */
class TeamSprintScenarioTest extends ApiTest {

    @Test
    void aTeamRunsASprintAcrossItsProjects() throws Exception {
        TestUser anita = newUser();   // manager
        TestUser vikram = newUser();  // second manager
        TestUser rahul = newUser();   // member
        TestUser priya = newUser();   // member

        // The admin creates Team Alpha with Anita as its manager.
        UUID alpha = createTeam(anita);

        // Anita adds Vikram as a second manager, and Rahul and Priya as members.
        addMember(anita, alpha, vikram, "MANAGER");
        addMember(anita, alpha, rahul, "MEMBER");
        addMember(anita, alpha, priya, "MEMBER");

        // Anita creates two projects; Vikram leads the second one.
        TestProject payments = createProject(anita, alpha);
        TestProject search = createProjectLedBy(vikram, alpha);

        // Anita plans the next sprint.
        UUID sprint = createSprint(anita, alpha);

        // Rahul creates tasks in both projects; some are for Priya.
        String refunds = createTask(rahul, payments, "Refund API", priya);
        String payouts = createTask(rahul, payments, "Payout API", rahul);
        String ranking = createTask(rahul, search, "Ranking", priya);
        String cleanup = createTask(rahul, search, "Old index cleanup", null); // stays in the backlog

        // The tasks for this sprint are tagged into it; Anita starts it.
        for (String key : new String[] {refunds, payouts, ranking}) {
            call(rahul, post("/api/v1/tasks/{key}/sprints/{sprintId}", key, sprint), null)
                    .andExpect(status().isOk());
        }
        call(anita, post("/api/v1/sprints/{id}/start", sprint), null)
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Priya works on her tasks and finishes one; Rahul leaves her a comment.
        changeStatus(priya, refunds, "IN_PROGRESS");
        changeStatus(priya, refunds, "DONE");
        changeStatus(priya, ranking, "IN_PROGRESS");
        call(rahul, post("/api/v1/tasks/{key}/comments", ranking), "{\"body\": \"Try the BM25 settings\"}")
                .andExpect(status().isCreated());

        // Everyone in the team sees the sprint's progress, per project.
        call(vikram, get("/api/v1/sprints/{id}/page", sprint), null)
                .andExpect(jsonPath("$.done").value(1))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.projects.length()").value(2));

        // Anita plans the next sprint and completes this one, carrying the unfinished work over.
        UUID nextSprint = createSprint(anita, alpha);
        call(anita, post("/api/v1/sprints/{id}/complete", sprint),
                "{\"carryOverToSprintId\": \"" + nextSprint + "\"}")
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // The finished sprint still shows everything that was in it, done or not...
        call(rahul, get("/api/v1/sprints/{id}/page", sprint), null)
                .andExpect(jsonPath("$.projects[*].tasks[*].key", containsInAnyOrder(refunds, payouts, ranking)))
                .andExpect(jsonPath("$.done").value(1));
        // ...and the unfinished work continues in the next one.
        call(rahul, get("/api/v1/sprints/{id}/page", nextSprint), null)
                .andExpect(jsonPath("$.projects[*].tasks[*].key", containsInAnyOrder(payouts, ranking)));
        // The task nobody put in a sprint is still the only thing in its backlog.
        call(rahul, get("/api/v1/projects/{id}/backlog", search.id()), null)
                .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(cleanup)));

        // Someone from another team can't see any of it.
        TestUser outsider = newUser();
        call(outsider, get("/api/v1/teams/{id}", alpha), null).andExpect(status().isNotFound());
        call(outsider, get("/api/v1/tasks/{key}", refunds), null).andExpect(status().isNotFound());
        call(outsider, get("/api/v1/sprints/{id}", sprint), null).andExpect(status().isNotFound());
    }

    // --- helpers ---

    /** Sends the request as this user, with an optional JSON body. */
    private ResultActions call(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
        request.header("Authorization", user.token());
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }

    private void addMember(TestUser manager, UUID teamId, TestUser user, String role) throws Exception {
        call(manager, post("/api/v1/teams/{id}/members", teamId),
                "{\"email\": \"" + user.email() + "\", \"role\": \"" + role + "\"}")
                .andExpect(status().isCreated());
    }

    private TestProject createProjectLedBy(TestUser lead, UUID teamId) throws Exception {
        String code = uniqueProjectCode();
        String body = call(lead, post("/api/v1/teams/{id}/projects", teamId),
                "{\"code\": \"" + code + "\", \"name\": \"Search\", \"leadUserId\": \"" + lead.id() + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new TestProject(UUID.fromString(JsonPath.read(body, "$.id")), code);
    }

    private String createTask(TestUser creator, TestProject project, String title, TestUser assignee)
            throws Exception {
        String assignees = assignee == null ? "" : "\"" + assignee.id() + "\"";
        String body = call(creator, post("/api/v1/projects/{id}/tasks", project.id()),
                "{\"title\": \"" + title + "\", \"assigneeIds\": [" + assignees + "]}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private void changeStatus(TestUser user, String key, String status) throws Exception {
        call(user, put("/api/v1/tasks/{key}/status", key), "{\"status\": \"" + status + "\"}")
                .andExpect(status().isOk());
    }
}
