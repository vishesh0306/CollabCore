package com.collabflow.notification;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.collabflow.ApiTest;
import com.collabflow.shared.CompanyTime;
import com.collabflow.task.OverdueReminders;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** The reminder that goes out when a task's expected date has passed. */
class OverdueReminderTest extends ApiTest {

    @Autowired
    private OverdueReminders reminders;

    private TestUser manager;
    private TestUser assignee;
    private TestProject project;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        assignee = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, assignee, "MEMBER");
        project = createProject(manager, teamId);
        clearNotifications(manager, assignee);
    }

    @Test
    void thePeopleOnAnOverdueTaskAreRemindedOnlyOnce() throws Exception {
        LocalDate yesterday = CompanyTime.today().minusDays(1);
        String taskKey = createTask(yesterday);
        clearNotifications(assignee);

        remindEverybody();

        notifications(assignee)
                .andExpect(jsonPath("$.items[0].type").value("TASK_OVERDUE"))
                .andExpect(jsonPath("$.items[0].message", containsString(taskKey + " is overdue")))
                .andExpect(jsonPath("$.items[0].link").value("/tasks/" + taskKey));

        remindEverybody(); // the task is still overdue, but it was already announced

        unreadCount(assignee).andExpect(jsonPath("$.unread").value(1));
    }

    @Test
    void aFinishedTaskIsNotRemindedAbout() throws Exception {
        String taskKey = createTask(CompanyTime.today().minusDays(3));
        mockMvc.perform(put("/api/v1/tasks/{key}/status", taskKey)
                        .header("Authorization", assignee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isOk());
        clearNotifications(assignee);

        remindEverybody();

        unreadCount(assignee).andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void movingTheDateAndMissingItAgainRemindsAgain() throws Exception {
        String taskKey = createTask(CompanyTime.today().minusDays(2));
        remindEverybody();
        clearNotifications(assignee);

        // The manager gives it one more day, and that day passes too.
        mockMvc.perform(put("/api/v1/tasks/{key}", taskKey)
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\", \"expectedDate\": \""
                                + CompanyTime.today().minusDays(1) + "\"}"))
                .andExpect(status().isOk());

        remindEverybody();

        unreadCount(assignee).andExpect(jsonPath("$.unread").value(1));
    }

    // --- helpers ---

    /** Works through every overdue task, the way the scheduled job does. */
    private void remindEverybody() {
        while (reminders.remindNextBatch() > 0) {
            // keep going until there is nothing left to remind about
        }
    }

    private String createTask(LocalDate expectedDate) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\", \"expectedDate\": \"" + expectedDate
                                + "\", \"assigneeIds\": [\"" + assignee.id() + "\"]}"))
                .andExpect(status().isCreated())
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
