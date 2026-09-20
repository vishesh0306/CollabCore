package com.collabflow.notification;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.collabflow.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CommentNotificationsTest extends ApiTest {

    private TestUser manager;
    private TestUser author;     // creates the task
    private TestUser assignee;
    private String taskKey;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        author = newUser();
        assignee = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, author, "MEMBER");
        addToTeam(teamId, assignee, "MEMBER");
        TestProject project = createProject(manager, teamId);

        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\", \"assigneeIds\": [\"" + assignee.id() + "\"]}"))
                .andReturn().getResponse().getContentAsString();
        taskKey = JsonPath.read(body, "$.key");
        markAllRead(assignee); // clear the "you were assigned" one, so each test starts clean
    }

    @Test
    void aCommentNotifiesTheTasksPeopleButNotTheCommenter() throws Exception {
        comment(manager, "Any update on this?");

        notifications(author)
                .andExpect(jsonPath("$.items[0].type").value("TASK_COMMENTED"))
                .andExpect(jsonPath("$.items[0].message", containsString("commented on " + taskKey)))
                .andExpect(jsonPath("$.items[0].link").value("/tasks/" + taskKey));
        unreadCount(assignee).andExpect(jsonPath("$.unread").value(1));
        unreadCount(manager).andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void mentioningSomeoneByNameNotifiesThemOnlyOnce() throws Exception {
        // The assignee would normally get a "commented" notification, but a mention is more
        // specific, so they get that one instead, not both.
        comment(manager, "@Test User please look, actually @" + assignee.email().split("@")[0] + " can you check?");

        unreadCount(assignee).andExpect(jsonPath("$.unread").value(1)); // one, not two
        notifications(assignee)
                .andExpect(jsonPath("$.items[0].type").value("MENTIONED"))
                .andExpect(jsonPath("$.items[0].message", containsString("mentioned you in a comment")));
    }

    @Test
    void mentioningSomeoneOutsideTheTeamNotifiesNobody() throws Exception {
        TestUser outsider = newUser();

        comment(manager, "@" + outsider.email().split("@")[0] + " have a look");

        unreadCount(outsider).andExpect(jsonPath("$.unread").value(0));
        unreadCount(assignee).andExpect(jsonPath("$.unread").value(1)); // the plain "commented" one
    }

    // --- helpers ---

    private void comment(TestUser user, String body) throws Exception {
        mockMvc.perform(post("/api/v1/tasks/{key}/comments", taskKey)
                        .header("Authorization", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"" + body + "\"}"))
                .andExpect(status().isCreated());
    }

    private ResultActions notifications(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications").header("Authorization", user.token()))
                .andExpect(status().isOk());
    }

    private ResultActions unreadCount(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", user.token()))
                .andExpect(status().isOk());
    }

    private void markAllRead(TestUser user) throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all").header("Authorization", user.token()));
    }
}
