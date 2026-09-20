package com.collabflow.audit;

import static org.assertj.core.api.Assertions.assertThat;
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

/** The log written for projects and sprints, including changes nobody made by hand. */
class ProjectSprintAuditTest extends ApiTest {

    @Autowired
    private AuditRepository auditRepository;

    private TestUser manager;
    private UUID teamId;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        teamId = createTeam(manager);
    }

    @Test
    void aProjectsWholeLifeIsWrittenDown() throws Exception {
        TestProject project = createProject(manager, teamId);

        mockMvc.perform(put("/api/v1/projects/{id}", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Payments v2\", \"leadUserId\": \"" + manager.id() + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/projects/{id}/complete", project.id())
                .header("Authorization", manager.token())).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/projects/{id}/reopen", project.id())
                .header("Authorization", manager.token())).andExpect(status().isOk());

        List<AuditEntry> history = historyOf(AuditEntityType.PROJECT, project.id());
        assertThat(history).extracting(AuditEntry::getAction).containsExactly(
                AuditAction.REOPENED, AuditAction.COMPLETED, AuditAction.UPDATED, AuditAction.CREATED);
        assertThat(history).allSatisfy(entry -> {
            assertThat(entry.getEntityLabel()).isEqualTo(project.code());
            assertThat(entry.getActorId()).isEqualTo(manager.id());
        });
        assertThat(history.get(2).getChanges()).extracting(FieldChange::field).containsExactly("name");
    }

    @Test
    void aSprintsPlanningAndItsStartAreWrittenDown() throws Exception {
        UUID sprintId = createSprint(manager, teamId);

        mockMvc.perform(put("/api/v1/sprints/{id}", sprintId)
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sprint 6\", \"target\": \"Ship refunds\","
                                + " \"startDate\": \"2026-10-01\", \"endDate\": \"2026-10-20\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/sprints/{id}/start", sprintId)
                .header("Authorization", manager.token())).andExpect(status().isOk());

        List<AuditEntry> history = historyOf(AuditEntityType.SPRINT, sprintId);
        assertThat(history).extracting(AuditEntry::getAction)
                .containsExactly(AuditAction.STARTED, AuditAction.UPDATED, AuditAction.CREATED);
        assertThat(history.get(1).getChanges()).extracting(FieldChange::field)
                .containsExactly("name", "target", "endDate"); // the start date was already 1 Oct
    }

    @Test
    void tasksPushedBackByAFinishedSprintAreLoggedWithNoActor() throws Exception {
        TestProject project = createProject(manager, teamId);
        UUID sprintId = createSprint(manager, teamId);
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Unfinished work\"}"))
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));
        String key = JsonPath.read(body, "$.key");
        mockMvc.perform(put("/api/v1/tasks/{key}/sprint", key).header("Authorization", manager.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"sprintId\": \"" + sprintId + "\"}"));
        mockMvc.perform(post("/api/v1/sprints/{id}/start", sprintId).header("Authorization", manager.token()));

        mockMvc.perform(post("/api/v1/sprints/{id}/complete", sprintId)
                .header("Authorization", manager.token())).andExpect(status().isOk());

        AuditEntry pushedBack = historyOf(AuditEntityType.TASK, taskId).get(0);
        assertThat(pushedBack.getAction()).isEqualTo(AuditAction.MOVED_TO_SPRINT);
        assertThat(pushedBack.getActorId()).isNull(); // nobody moved it; the sprint ending did
        assertThat(pushedBack.getChanges()).containsExactly(new FieldChange("sprint", "Sprint", "Backlog"));
    }

    private List<AuditEntry> historyOf(AuditEntityType type, UUID id) {
        return auditRepository.findByEntityTypeAndEntityIdOrderByIdDesc(type, id);
    }
}
