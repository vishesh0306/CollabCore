package com.collabflow.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.collabflow.ApiTest;
import com.collabflow.shared.FieldChange;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** The log written for teams, for who belongs to them, and for comments. */
class TeamCommentAuditTest extends ApiTest {

    @Autowired
    private AuditRepository auditRepository;

    private TestUser manager;
    private TestUser member;
    private UUID teamId;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        teamId = createTeam(manager);
    }

    @Test
    void creatingATeamIsWrittenDown() {
        List<AuditEntry> history = historyOf(AuditEntityType.TEAM, teamId);

        assertThat(history).extracting(AuditEntry::getAction).containsExactly(AuditAction.CREATED);
        assertThat(history.get(0).getChanges()).extracting(FieldChange::field).containsExactly("name");
    }

    @Test
    void joiningLeavingAndChangingRoleAreAllWrittenDown() throws Exception {
        addToTeam(teamId, member, "MEMBER");
        mockMvc.perform(put("/api/v1/teams/{teamId}/members/{userId}", teamId, member.id())
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"MANAGER\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/teams/{teamId}/members/{userId}", teamId, member.id())
                .header("Authorization", adminToken())).andExpect(status().isNoContent());

        List<AuditEntry> history = historyOf(AuditEntityType.TEAM_MEMBER, member.id());
        assertThat(history).extracting(AuditEntry::getAction).containsExactly(
                AuditAction.MEMBER_REMOVED, AuditAction.ROLE_CHANGED, AuditAction.MEMBER_ADDED);
        assertThat(history).allSatisfy(entry -> assertThat(entry.getTeamId()).isEqualTo(teamId));
        assertThat(history.get(1).getChanges())
                .containsExactly(new FieldChange("role", "MEMBER", "MANAGER"));
    }

    @Test
    void writingEditingAndDeletingACommentAreWrittenDownWithoutItsText() throws Exception {
        TestProject project = createProject(manager, teamId);
        String taskBody = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\"}"))
                .andReturn().getResponse().getContentAsString();
        String key = JsonPath.read(taskBody, "$.key");

        String commentBody = mockMvc.perform(post("/api/v1/tasks/{key}/comments", key)
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Looks wrong to me\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID commentId = UUID.fromString(JsonPath.read(commentBody, "$.id"));

        mockMvc.perform(put("/api/v1/comments/{id}", commentId).header("Authorization", manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"My mistake, looks right\"}"));
        mockMvc.perform(delete("/api/v1/comments/{id}", commentId).header("Authorization", manager.token()));

        List<AuditEntry> history = historyOf(AuditEntityType.COMMENT, commentId);
        assertThat(history).extracting(AuditEntry::getAction)
                .containsExactly(AuditAction.DELETED, AuditAction.UPDATED, AuditAction.CREATED);
        assertThat(history).allSatisfy(entry -> {
            assertThat(entry.getEntityLabel()).isEqualTo(key); // which task it was on
            assertThat(entry.getTeamId()).isEqualTo(teamId);
            assertThat(entry.getChanges()).isEmpty(); // the text stays in the comments table
        });
    }

    private List<AuditEntry> historyOf(AuditEntityType type, UUID id) {
        return auditRepository.findByEntityTypeAndEntityIdOrderByIdDesc(type, id);
    }
}
