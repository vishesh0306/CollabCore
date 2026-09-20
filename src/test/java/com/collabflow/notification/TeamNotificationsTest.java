package com.collabflow.notification;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.collabflow.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class TeamNotificationsTest extends ApiTest {

    @Test
    void joiningATeamNotifiesTheNewMember() throws Exception {
        TestUser manager = newUser();
        TestUser member = newUser();
        UUID teamId = createTeam(manager);

        addToTeam(teamId, member, "MEMBER");

        notifications(member)
                .andExpect(jsonPath("$.items[0].type").value("ADDED_TO_TEAM"))
                .andExpect(jsonPath("$.items[0].message", containsString("added you to the team")))
                .andExpect(jsonPath("$.items[0].link").value("/teams/" + teamId));
    }

    @Test
    void theFirstManagerIsToldWhenTheTeamIsCreatedForThem() throws Exception {
        TestUser manager = newUser();

        UUID teamId = createTeam(manager);

        unreadCount(manager).andExpect(jsonPath("$.unread").value(1));
        notifications(manager).andExpect(jsonPath("$.items[0].link").value("/teams/" + teamId));
    }

    @Test
    void startingASprintNotifiesTheTeamButNotWhoeverStartedIt() throws Exception {
        TestUser manager = newUser();
        TestUser member = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        UUID sprintId = createSprint(manager, teamId);
        clearNotifications(manager, member);

        mockMvc.perform(post("/api/v1/sprints/{id}/start", sprintId)
                        .header("Authorization", manager.token()))
                .andExpect(status().isOk());

        notifications(member)
                .andExpect(jsonPath("$.items[0].type").value("SPRINT_STARTED"))
                .andExpect(jsonPath("$.items[0].message", containsString("started Sprint")))
                .andExpect(jsonPath("$.items[0].link").value("/sprints/" + sprintId));
        unreadCount(manager).andExpect(jsonPath("$.unread").value(0));
    }

    // --- helpers ---

    private ResultActions notifications(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications").header("Authorization", user.token()))
                .andExpect(status().isOk());
    }

    private ResultActions unreadCount(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", user.token()))
                .andExpect(status().isOk());
    }
}
