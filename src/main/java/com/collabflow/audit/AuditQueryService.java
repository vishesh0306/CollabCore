package com.collabflow.audit;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.collabflow.audit.dto.AuditEntryResponse;
import com.collabflow.audit.dto.AuditPage;
import com.collabflow.identity.UserService;
import com.collabflow.project.Project;
import com.collabflow.project.ProjectService;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.task.Task;
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
    private final TaskService taskService;
    private final ProjectService projectService;

    /** TSK-8: one task's full history, oldest change last. Not paged: a task has few entries. */
    @Transactional(readOnly = true)
    public List<AuditEntryResponse> taskHistory(UUID callerId, String key) {
        Task task = taskService.findVisibleTask(key, callerId);
        return describe(auditRepository.findByEntityTypeAndEntityIdOrderByIdDesc(
                AuditEntityType.TASK, task.getId()));
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
