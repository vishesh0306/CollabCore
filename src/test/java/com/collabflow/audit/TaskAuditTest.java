package com.collabflow.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

/** The log written for tasks, and the two rules that make a log worth trusting. */
@Import(AuditTestBeans.class)
class TaskAuditTest extends ApiTest {

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private FailingWork failingWork;

    @Autowired
    private JdbcTemplate jdbc;

    private TestUser manager;
    private TestUser member;
    private UUID teamId;
    private TestProject project;

    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        project = createProject(manager, teamId);
    }

    @Test
    void everyChangeToATaskIsWrittenDown() throws Exception {
        UUID sprintId = createSprint(manager, teamId);
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\", \"assigneeIds\": [\"" + member.id() + "\"]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));
        String key = JsonPath.read(body, "$.key");

        change(put("/api/v1/tasks/{key}", key),
                "{\"title\": \"Refund API v2\", \"description\": \"Partial refunds\"}");
        change(put("/api/v1/tasks/{key}/status", key), "{\"status\": \"IN_PROGRESS\"}");
        tagIntoSprint(key, sprintId);
        change(put("/api/v1/tasks/{key}/assignees", key), "{\"assigneeIds\": []}");
        mockMvc.perform(delete("/api/v1/tasks/{key}", key).header("Authorization", manager.token()))
                .andExpect(status().isNoContent());

        List<AuditEntry> history = historyOf(taskId);
        assertThat(history).extracting(AuditEntry::getAction).containsExactly(
                AuditAction.DELETED,
                AuditAction.ASSIGNEES_CHANGED,
                AuditAction.TAGGED_INTO_SPRINT,
                AuditAction.STATUS_CHANGED,
                AuditAction.UPDATED,
                AuditAction.ASSIGNEES_CHANGED,
                AuditAction.CREATED);
        assertThat(history).allSatisfy(entry -> {
            assertThat(entry.getEntityLabel()).isEqualTo(key);
            assertThat(entry.getTeamId()).isEqualTo(teamId);
            assertThat(entry.getActorId()).isEqualTo(manager.id());
            assertThat(entry.getAt()).isNotNull();
        });
    }

    @Test
    void itStoresTheOldAndTheNewValue() throws Exception {
        String key = createTask("Refund API");
        UUID taskId = taskIdOf(key);

        change(put("/api/v1/tasks/{key}/status", key), "{\"status\": \"IN_PROGRESS\"}");

        assertThat(historyOf(taskId).get(0).getChanges())
                .containsExactly(new FieldChange("status", "TO_DO", "IN_PROGRESS"));
    }

    @Test
    void taggingAndUntaggingASprintAreBothWrittenDown() throws Exception {
        UUID sprintId = createSprint(manager, teamId);
        String key = createTask("Refund API");
        UUID taskId = taskIdOf(key);

        tagIntoSprint(key, sprintId);
        assertThat(historyOf(taskId).get(0).getAction()).isEqualTo(AuditAction.TAGGED_INTO_SPRINT);
        assertThat(historyOf(taskId).get(0).getChanges())
                .containsExactly(new FieldChange("sprint", null, "Sprint"));

        mockMvc.perform(delete("/api/v1/tasks/{key}/sprints/{sprintId}", key, sprintId)
                .header("Authorization", manager.token())).andExpect(status().isOk());

        assertThat(historyOf(taskId).get(0).getAction()).isEqualTo(AuditAction.UNTAGGED_FROM_SPRINT);
        assertThat(historyOf(taskId).get(0).getChanges())
                .containsExactly(new FieldChange("sprint", "Sprint", null));
    }

    @Test
    void oneRequestThatChangesThreeFieldsIsOneEntry() throws Exception {
        String key = createTask("Refund API");
        UUID taskId = taskIdOf(key);

        change(put("/api/v1/tasks/{key}", key),
                "{\"title\": \"Refunds\", \"description\": \"Partial\", \"expectedDate\": \"2026-10-02\"}");

        AuditEntry entry = historyOf(taskId).get(0);
        assertThat(entry.getAction()).isEqualTo(AuditAction.UPDATED);
        assertThat(entry.getChanges()).extracting(FieldChange::field)
                .containsExactly("title", "description", "expectedDate");
    }

    @Test
    void aFieldThatDidNotChangeIsNotWrittenDown() throws Exception {
        String key = createTask("Refund API");
        UUID taskId = taskIdOf(key);

        // The same title as before; only the description is new.
        change(put("/api/v1/tasks/{key}", key),
                "{\"title\": \"Refund API\", \"description\": \"Partial\"}");

        assertThat(historyOf(taskId).get(0).getChanges())
                .extracting(FieldChange::field).containsExactly("description");
    }

    // --- the rules that make the log worth trusting ---

    @Test
    void anEntryCannotBeWrittenWithoutAChangeAroundIt() {
        // Propagation.MANDATORY: there is no way to log something outside a transaction.
        assertThatThrownBy(() -> auditService.record(manager.id(), teamId, AuditEntityType.TASK,
                UUID.randomUUID(), "PAY-1", AuditAction.CREATED, List.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void anEntryIsRolledBackTogetherWithTheChangeItDescribes() {
        long before = auditRepository.count();

        assertThatThrownBy(() -> failingWork.logSomethingThenFail(manager.id(), teamId))
                .hasMessage("the change failed after the log was written");

        assertThat(auditRepository.count()).isEqualTo(before); // LOG-4: never one without the other
    }

    @Test
    void theDatabaseRefusesToChangeOrDeleteAnEntry() throws Exception {
        UUID taskId = taskIdOf(createTask("Refund API"));
        Long id = historyOf(taskId).get(0).getId();

        assertThatThrownBy(() -> jdbc.update("UPDATE audit_log SET action = 'DELETED' WHERE id = ?", id))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log WHERE id = ?", id))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE audit_log"))
                .hasMessageContaining("append-only");
    }

    // --- helpers ---

    private List<AuditEntry> historyOf(UUID taskId) {
        return auditRepository.findByEntityTypeAndEntityIdOrderByIdDesc(AuditEntityType.TASK, taskId);
    }

    private void tagIntoSprint(String key, UUID sprintId) throws Exception {
        mockMvc.perform(post("/api/v1/tasks/{key}/sprints/{sprintId}", key, sprintId)
                .header("Authorization", manager.token())).andExpect(status().isOk());
    }

    private void change(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                        String json) throws Exception {
        mockMvc.perform(request.header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    private String createTask(String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private UUID taskIdOf(String key) throws Exception {
        String body = mockMvc.perform(get("/api/v1/tasks/{key}", key).header("Authorization", manager.token()))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
