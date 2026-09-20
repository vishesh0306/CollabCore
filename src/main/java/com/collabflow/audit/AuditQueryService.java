package com.collabflow.audit;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.collabflow.audit.dto.AuditEntryResponse;
import com.collabflow.audit.dto.AuditPage;
import com.collabflow.identity.UserService;
import com.collabflow.project.Project;
import com.collabflow.project.ProjectService;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.sprint.Sprint;
import com.collabflow.sprint.SprintService;
import com.collabflow.task.TaskService;
import com.collabflow.team.TeamAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading the log (LOG-5, LOG-6). A team's people see their team's entries; the admin sees
 * everything. Permission is decided by the item being asked about, exactly as everywhere else.
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private final AuditRepository auditRepository;
    private final UserService userService;
    private final TeamAccess teamAccess;
    private final ProjectService projectService;
    private final SprintService sprintService;
    private final TaskService taskService;

    /**
     * TSK-8: one task's full history, newest first, including its comments. Not paged: a task
     * has few entries.
     *
     * <p>Read from the log by the task's key, not by loading the task, so a deleted task still
     * answers for what was done to it. Permission comes from the team on the entries themselves.
     */
    @Transactional(readOnly = true)
    public List<AuditEntryResponse> taskHistory(UUID callerId, String key) {
        List<AuditEntry> entries = auditRepository.findByEntityLabelAndEntityTypeInOrderByIdDesc(
                key.toUpperCase(Locale.ROOT), List.of(AuditEntityType.TASK, AuditEntityType.COMMENT));
        if (entries.isEmpty() || !teamAccess.canView(entries.get(0).getTeamId(), callerId)) {
            throw new NotFoundException("Task not found");
        }
        return describe(entries);
    }

    /** Everything that happened in a team, newest first. */
    @Transactional(readOnly = true)
    public AuditPage teamActivity(UUID callerId, UUID teamId, Long before, Integer size) {
        teamAccess.requireVisible(teamId, callerId);
        return page(AuditFilters.forTeam(teamId, before), size);
    }

    /** A project, its tasks and their comments, newest first. */
    @Transactional(readOnly = true)
    public AuditPage projectActivity(UUID callerId, UUID projectId, Long before, Integer size) {
        Project project = projectService.findVisibleProject(projectId, callerId);
        return page(AuditFilters.forProject(project.getId(), before), size);
    }

    /**
     * A sprint's timeline (SPR-6): the sprint's own entries and everything that happened to the
     * tasks tagged into it, newest first. Tasks tagged in later bring their whole history with
     * them, which is the honest answer to "what went on in this sprint".
     */
    @Transactional(readOnly = true)
    public AuditPage sprintTimeline(UUID callerId, UUID sprintId, Long before, Integer size) {
        Sprint sprint = sprintService.findVisibleSprint(sprintId, callerId);
        List<String> taskKeys = taskService.taskKeysInSprint(sprint.getId());
        int limit = pageSize(size);
        List<AuditEntry> entries = auditRepository.findBy(
                AuditFilters.forSprint(sprint.getId(), taskKeys, before),
                query -> query.sortBy(Sort.by(Sort.Direction.DESC, "id")).limit(limit).all());
        return AuditPage.of(describe(entries), limit);
    }

    /** The admin's company-wide log, with every filter optional. */
    @Transactional(readOnly = true)
    public AuditPage companyLog(UUID callerId, UUID teamId, UUID actorId, AuditEntityType entityType,
                                UUID entityId, AuditAction action, Instant from, Instant to,
                                Long before, Integer size) {
        teamAccess.requireAdmin(callerId);
        return page(new AuditFilters(teamId, null, actorId, entityType, entityId, action, from, to, before),
                size);
    }

    /**
     * Reads one page: newest first, no more than the asked-for size, and no count query. The
     * fluent findBy avoids the count that a Page would run on every request.
     */
    private AuditPage page(AuditFilters filters, Integer size) {
        int limit = pageSize(size);
        List<AuditEntry> entries = auditRepository.findBy(filters.toSpecification(),
                query -> query.sortBy(Sort.by(Sort.Direction.DESC, "id")).limit(limit).all());
        return AuditPage.of(describe(entries), limit);
    }

    /** Turns entries into responses, looking up all the actors' names in one query. */
    private List<AuditEntryResponse> describe(List<AuditEntry> entries) {
        Set<UUID> actorIds = new HashSet<>();
        entries.forEach(entry -> {
            if (entry.getActorId() != null) {
                actorIds.add(entry.getActorId());
            }
        });
        Map<UUID, String> names = userService.namesOf(actorIds);
        return entries.stream().map(entry -> AuditEntryResponse.from(entry, names)).toList();
    }

    private static int pageSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_SIZE);
        }
        return size;
    }
}
