package com.collabflow.task;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
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

/** Sprints as tags: a task can be in several at once, and a finished sprint keeps its tasks. */
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
    void aMemberTagsTheirTaskIntoASprintAndTakesItOffAgain() throws Exception {
        String key = createTask(member, payments);

        tagIntoSprint(member, key, sprintId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprints[0].id").value(sprintId.toString()));
        backlog(payments).andExpect(jsonPath("$.totalItems").value(0));

        untagFromSprint(member, key, sprintId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprints", hasSize(0)));
        backlog(payments).andExpect(jsonPath("$.items[0].key").value(key));
    }

    @Test
    void aTaskCanBeInSeveralSprintsAtOnce() throws Exception {
        String key = createTask(member, payments);
        UUID secondSprint = createSprint(manager, teamId);

        tagIntoSprint(member, key, sprintId);
        tagIntoSprint(member, key, secondSprint).andExpect(jsonPath("$.sprints", hasSize(2)));

        // It shows up in both sprints, not just the newer one.
        sprintTasks(sprintId).andExpect(jsonPath("$.projects[0].tasks[0].key").value(key));
        sprintTasks(secondSprint).andExpect(jsonPath("$.projects[0].tasks[0].key").value(key));
    }

    @Test
    void taggingTheSameSprintTwiceChangesNothing() throws Exception {
        String key = createTask(member, payments);

        tagIntoSprint(member, key, sprintId);
        tagIntoSprint(member, key, sprintId).andExpect(jsonPath("$.sprints", hasSize(1)));
        sprintTasks(sprintId).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void membersCannotTagOtherPeoplesTasks() throws Exception {
        String key = createTask(member, payments);

        tagIntoSprint(otherMember, key, sprintId).andExpect(status().isForbidden());
    }

    @Test
    void aTaskOnlyGoesIntoItsOwnTeamsSprints() throws Exception {
        String key = createTask(member, payments);
        TestUser otherManager = newUser();
        UUID otherTeamsSprint = createSprint(otherManager, createTeam(otherManager));

        tagIntoSprint(member, key, otherTeamsSprint)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The sprint must belong to the task's team"));
    }

    @Test
    void theSprintViewGroupsTasksByProjectWithProgress() throws Exception {
        String pay1 = createTask(member, payments);
        String pay2 = createTask(member, payments);
        String srch1 = createTask(member, search);
        for (String key : new String[] {pay1, pay2, srch1}) {
            tagIntoSprint(member, key, sprintId);
        }
        changeStatus(pay1, "DONE");

        sprintTasks(sprintId)
                .andExpect(jsonPath("$.done").value(1))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.projects[?(@.code == '" + payments.code() + "')].done").value(1))
                .andExpect(jsonPath("$.projects[?(@.code == '" + payments.code() + "')].total").value(2))
                .andExpect(jsonPath("$.projects[?(@.code == '" + search.code() + "')].total").value(1));
    }

    @Test
    void completingASprintCarriesUnfinishedWorkIntoTheNextOne() throws Exception {
        String done = createTask(member, payments);
        String unfinished = createTask(member, payments);
        tagIntoSprint(member, done, sprintId);
        tagIntoSprint(member, unfinished, sprintId);
        changeStatus(done, "DONE");
        UUID nextSprint = createSprint(manager, teamId);

        sprintAction("start");
        complete(nextSprint).andExpect(status().isOk());

        // The unfinished task continues in the next sprint...
        sprintTasks(nextSprint).andExpect(jsonPath("$.projects[0].tasks[*].key",
                containsInAnyOrder(unfinished)));
        // ...while the finished sprint still shows everything that was in it.
        sprintTasks(sprintId).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.done").value(1));
    }

    @Test
    void completingWithoutANextSprintLeavesTheTagsAlone() throws Exception {
        String unfinished = createTask(member, payments);
        tagIntoSprint(member, unfinished, sprintId);

        sprintAction("start");
        complete(null).andExpect(status().isOk());

        sprintTasks(sprintId).andExpect(jsonPath("$.total").value(1));
        backlog(payments).andExpect(jsonPath("$.totalItems").value(0)); // still tagged, not in the backlog
    }

    @Test
    void aFinishedSprintStaysEditable() throws Exception {
        String forgotten = createTask(member, payments);
        tagIntoSprint(member, forgotten, sprintId);
        sprintAction("start");
        complete(null);

        // Someone forgot to close it during the sprint; they still can.
        mockMvc.perform(put("/api/v1/tasks/{key}/status", forgotten)
                        .header("Authorization", member.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\"}"))
                .andExpect(status().isOk());
        sprintTasks(sprintId).andExpect(jsonPath("$.done").value(1));

        // And a task missed at the time can still be tagged in.
        String missed = createTask(member, payments);
        tagIntoSprint(member, missed, sprintId).andExpect(status().isOk());
        sprintTasks(sprintId).andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void theSprintPageShowsWhatTheSprintIsForAndHowLongIsLeft() throws Exception {
        sprintTasks(sprintId)
                .andExpect(jsonPath("$.name").value("Sprint"))
                .andExpect(jsonPath("$.target").value("Ship it"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.endDate").value("2026-10-15"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.daysLeft").isNumber());
    }

    @Test
    void aFinishedSprintHasNoDaysLeft() throws Exception {
        sprintAction("start");
        complete(null);

        sprintTasks(sprintId)
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.daysLeft").doesNotExist());
    }

    @Test
    void theTaskListCanBeFilteredBySprintOrBacklog() throws Exception {
        String inSprint = createTask(member, payments);
        String inBacklog = createTask(member, search);
        tagIntoSprint(member, inSprint, sprintId);

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

    private ResultActions tagIntoSprint(TestUser user, String key, UUID sprint) throws Exception {
        return mockMvc.perform(post("/api/v1/tasks/{key}/sprints/{sprintId}", key, sprint)
                .header("Authorization", user.token()));
    }

    private ResultActions untagFromSprint(TestUser user, String key, UUID sprint) throws Exception {
        return mockMvc.perform(delete("/api/v1/tasks/{key}/sprints/{sprintId}", key, sprint)
                .header("Authorization", user.token()));
    }

    private ResultActions sprintTasks(UUID sprint) throws Exception {
        return mockMvc.perform(get("/api/v1/sprints/{id}/page", sprint)
                        .header("Authorization", otherMember.token()))
                .andExpect(status().isOk());
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

    private ResultActions complete(UUID carryOverTo) throws Exception {
        String body = carryOverTo == null ? "{}" : "{\"carryOverToSprintId\": \"" + carryOverTo + "\"}";
        return mockMvc.perform(post("/api/v1/sprints/{id}/complete", sprintId)
                .header("Authorization", manager.token())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions backlog(TestProject project) throws Exception {
        return mockMvc.perform(get("/api/v1/projects/{id}/backlog", project.id())
                .header("Authorization", member.token()));
    }
}
