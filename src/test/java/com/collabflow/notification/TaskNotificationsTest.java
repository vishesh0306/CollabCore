package com.collabflow.notification;

import static org.hamcrest.Matchers.containsString;
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

class TaskNotificationsTest extends ApiTest {

    private TestUser manager;
    private TestUser member;
    private TestUser otherMember;
    private TestProject project;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        otherMember = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        addToTeam(teamId, otherMember, "MEMBER");
        project = createProject(manager, teamId);
    }

    @Test
    void beingAssignedNotifiesYouButNotThePersonWhoDidIt() throws Exception {
        createTask(member, otherMember);

        unreadCount(otherMember).andExpect(jsonPath("$.unread").value(1));
        notifications(otherMember)
                .andExpect(jsonPath("$.items[0].type").value("TASK_ASSIGNED"))
                .andExpect(jsonPath("$.items[0].message", containsString("assigned you to")))
                .andExpect(jsonPath("$.items[0].read").value(false));
        unreadCount(member).andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void beingRemovedFromATaskNotifiesYouToo() throws Exception {
        String key = createTask(member, otherMember);

        mockMvc.perform(put("/api/v1/tasks/{key}/assignees", key)
                .header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeIds\": []}"));

        notifications(otherMember)
                .andExpect(jsonPath("$.items[0].type").value("TASK_UNASSIGNED"))
                .andExpect(jsonPath("$.totalItems").value(2)); // assigned, then unassigned
    }

    @Test
    void aStatusChangeNotifiesTheCreatorAndTheAssignees() throws Exception {
        String key = createTask(member, otherMember);

        mockMvc.perform(put("/api/v1/tasks/{key}/status", key)
                .header("Authorization", manager.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"IN_PROGRESS\"}"));

        notifications(member)
                .andExpect(jsonPath("$.items[0].type").value("TASK_STATUS_CHANGED"))
                .andExpect(jsonPath("$.items[0].message", containsString("from To Do to In Progress")))
                .andExpect(jsonPath("$.items[0].link").value("/tasks/" + key));
        unreadCount(otherMember).andExpect(jsonPath("$.unread").value(2)); // assigned + status
        unreadCount(manager).andExpect(jsonPath("$.unread").value(0));     // they did it
    }

    @Test
    void deletingATaskNotifiesThePeopleOnIt() throws Exception {
        String key = createTask(member, otherMember);

        mockMvc.perform(delete("/api/v1/tasks/{key}", key).header("Authorization", manager.token()));

        notifications(member)
                .andExpect(jsonPath("$.items[0].type").value("TASK_DELETED"))
                .andExpect(jsonPath("$.items[0].message", containsString("deleted " + key)));
    }

    @Test
    void theTabShowsUnreadOnesAndTheyCanBeMarkedRead() throws Exception {
        createTask(member, otherMember);
        createTask(member, otherMember);

        unreadCount(otherMember).andExpect(jsonPath("$.unread").value(2));
        String first = JsonPath.read(notifications(otherMember).andReturn().getResponse().getContentAsString(),
                "$.items[0].id");

        mockMvc.perform(post("/api/v1/notifications/{id}/read", first)
                        .header("Authorization", otherMember.token()))
                .andExpect(status().isNoContent());
        unreadCount(otherMember).andExpect(jsonPath("$.unread").value(1));
        mockMvc.perform(get("/api/v1/notifications?unread=true").header("Authorization", otherMember.token()))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(post("/api/v1/notifications/read-all").header("Authorization", otherMember.token()))
                .andExpect(status().isNoContent());
        unreadCount(otherMember).andExpect(jsonPath("$.unread").value(0));
        notifications(otherMember).andExpect(jsonPath("$.totalItems").value(2)); // still listed, just read
    }

    @Test
    void youCannotTouchSomeoneElsesNotifications() throws Exception {
        createTask(member, otherMember);
        String theirs = JsonPath.read(notifications(otherMember).andReturn().getResponse().getContentAsString(),
                "$.items[0].id");

        mockMvc.perform(post("/api/v1/notifications/{id}/read", theirs).header("Authorization", member.token()))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    /** Creates a task as the first user, assigned to the second, and returns its key. */
    private String createTask(TestUser creator, TestUser assignee) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", creator.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\", \"assigneeIds\": [\"" + assignee.id() + "\"]}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private ResultActions notifications(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications").header("Authorization", user.token()))
                .andExpect(status().isOk());
    }

    private ResultActions unreadCount(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", user.token()))
                .andExpect(status().isOk());
    }
}
