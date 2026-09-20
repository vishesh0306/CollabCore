package com.collabflow.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.collabflow.identity.User;
import com.collabflow.identity.UserService;
import com.collabflow.project.Project;
import com.collabflow.project.ProjectService;
import com.collabflow.shared.FieldChange;
import com.collabflow.shared.PageResponse;
import com.collabflow.sprint.Sprint;
import com.collabflow.sprint.SprintService;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.task.dto.ChangeStatusRequest;
import com.collabflow.task.dto.CreateTaskRequest;
import com.collabflow.task.dto.MoveToSprintRequest;
import com.collabflow.task.dto.ReplaceAssigneesRequest;
import com.collabflow.task.dto.SprintTasksResponse;
import com.collabflow.task.dto.TaskResponse;
import com.collabflow.task.dto.UpdateTaskRequest;
import com.collabflow.team.TeamAccess;
import com.collabflow.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tasks. Everyone in the team can see and create them. Managers (and the admin) can change
 * any task; members only their own (created by them or assigned to them).
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final SprintService sprintService;
    private final TeamAccess teamAccess;
    private final TeamMemberService teamMemberService;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Transactional
    public TaskResponse createTask(UUID callerId, UUID projectId, CreateTaskRequest request) {
        // Locks the project row until this transaction commits: a second task being created in
        // the same project at the same moment waits here, so both can't get the same number.
        Project project = projectService.findVisibleProjectForUpdate(projectId, callerId);
        ProjectService.requireActive(project);
        Set<User> assignees = findAssignees(project.getTeam().getId(), request.assigneeIds());
        User creator = userService.getById(callerId);

        Task task = new Task(project, project.takeNextTaskNumber(), request.title(), request.description(),
                request.expectedDate(), creator, assignees);
        taskRepository.saveAndFlush(task);

        List<FieldChange> initial = new ArrayList<>();
        initial.add(FieldChange.set("title", task.getTitle()));
        if (task.getDescription() != null) {
            initial.add(FieldChange.set("description", task.getDescription()));
        }
        if (task.getExpectedDate() != null) {
            initial.add(FieldChange.set("expectedDate", task.getExpectedDate()));
        }
        events.publishEvent(new TaskEvents.Created(task.getId(), task.getTeamId(), task.getKey(),
                task.getTitle(), callerId, initial));
        if (!assignees.isEmpty()) {
            events.publishEvent(new TaskEvents.AssigneesChanged(task.getId(), task.getTeamId(), task.getKey(),
                    task.getTitle(), callerId, idsOf(assignees), Set.of()));
        }
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID callerId, String key) {
        return TaskResponse.from(findVisibleTask(key, callerId));
    }

    /** Fields the list can be sorted by. Anything else would be a 500 from the database layer. */
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "expectedDate", "title", "number");

    /** A team's tasks, filtered and one page at a time. Newest first unless another sort is asked for. */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(UUID callerId, UUID teamId, TaskFilters filters, Pageable pageable) {
        teamAccess.requireVisible(teamId, callerId);
        return findPage(teamId, filters, pageable);
    }

    /** A project's backlog: its tasks that are in no sprint. */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getBacklog(UUID callerId, UUID projectId, Pageable pageable) {
        Project project = projectService.findVisibleProject(projectId, callerId);
        TaskFilters backlogOnly = new TaskFilters(projectId, null, true, null, null, null, null);
        return findPage(project.getTeam().getId(), backlogOnly, pageable);
    }

    /** A sprint's tasks grouped by project, with done/total counts. */
    @Transactional(readOnly = true)
    public SprintTasksResponse getSprintTasks(UUID callerId, UUID sprintId) {
        Sprint sprint = sprintService.findVisibleSprint(sprintId, callerId);
        return SprintTasksResponse.from(sprint.getId(), taskRepository.findInSprint(sprintId));
    }

    /**
     * Puts the task into a planned or active sprint of its team, or back into the backlog (null).
     * This counts as changing the task, so members can only move their own tasks.
     */
    @Transactional
    public TaskResponse moveToSprint(UUID callerId, String key, MoveToSprintRequest request) {
        Task task = findChangeableTask(key, callerId);
        if (task.getSprint() != null && !task.getSprint().isOpen()) {
            throw new ConflictException("This task is part of a completed sprint and stays there");
        }
        Sprint sprint = request.sprintId() == null
                ? null
                : sprintService.findOpenSprintOfTeam(request.sprintId(), task.getTeamId());
        String from = sprintName(task.getSprint());
        String to = sprintName(sprint);
        task.moveToSprint(sprint);
        if (!from.equals(to)) {
            events.publishEvent(new TaskEvents.MovedToSprint(task.getId(), task.getTeamId(), task.getKey(),
                    callerId, from, to));
        }
        return TaskResponse.from(task);
    }

    /** How the log should name where a task sits. */
    private static String sprintName(Sprint sprint) {
        return sprint == null ? "Backlog" : sprint.getName();
    }

    private static Set<UUID> idsOf(Collection<User> users) {
        return users.stream().map(User::getId).collect(Collectors.toSet());
    }

    private PageResponse<TaskResponse> findPage(UUID teamId, TaskFilters filters, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Can't sort by '" + order.getProperty()
                        + "'. Use one of: createdAt, expectedDate, title, number");
            }
        }
        Page<Task> page = taskRepository.findAll(filters.toSpecification(teamId), pageable);
        return PageResponse.from(page.map(TaskResponse::from));
    }

    @Transactional
    public TaskResponse updateDetails(UUID callerId, String key, UpdateTaskRequest request) {
        Task task = findChangeableTask(key, callerId);
        List<FieldChange> changes = new ArrayList<>();
        addIfChanged(changes, "title", task.getTitle(), request.title());
        addIfChanged(changes, "description", task.getDescription(), request.description());
        addIfChanged(changes, "expectedDate", task.getExpectedDate(), request.expectedDate());

        task.updateDetails(request.title(), request.description(), request.expectedDate());
        if (!changes.isEmpty()) {
            events.publishEvent(new TaskEvents.DetailsUpdated(task.getId(), task.getTeamId(), task.getKey(),
                    callerId, changes));
        }
        return TaskResponse.from(task);
    }

    private static void addIfChanged(List<FieldChange> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(FieldChange.of(field, before, after));
        }
    }

    @Transactional
    public TaskResponse changeStatus(UUID callerId, String key, ChangeStatusRequest request) {
        Task task = findChangeableTask(key, callerId);
        TaskStatus previous = task.getStatus();
        task.changeStatus(request.status());
        if (previous != request.status()) {
            events.publishEvent(new TaskEvents.StatusChanged(task.getId(), task.getTeamId(), task.getKey(),
                    task.getTitle(), callerId, previous, request.status(), task.participantIds()));
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse replaceAssignees(UUID callerId, String key, ReplaceAssigneesRequest request) {
        Task task = findChangeableTask(key, callerId);
        Set<UUID> before = idsOf(task.getAssignees());
        task.replaceAssignees(findAssignees(task.getTeamId(), request.assigneeIds()));
        Set<UUID> after = idsOf(task.getAssignees());

        Set<UUID> added = new HashSet<>(after);
        added.removeAll(before);
        Set<UUID> removed = new HashSet<>(before);
        removed.removeAll(after);
        if (!added.isEmpty() || !removed.isEmpty()) {
            events.publishEvent(new TaskEvents.AssigneesChanged(task.getId(), task.getTeamId(), task.getKey(),
                    task.getTitle(), callerId, added, removed));
        }
        return TaskResponse.from(task);
    }

    /** Soft delete: the row stays (and keeps its number), but the task is hidden from now on. */
    @Transactional
    public void deleteTask(UUID callerId, String key) {
        Task task = findChangeableTask(key, callerId);
        task.delete();
        events.publishEvent(new TaskEvents.Deleted(task.getId(), task.getTeamId(), task.getKey(),
                task.getTitle(), callerId, task.participantIds()));
    }

    /**
     * The task if the caller may change it. Managers (and the admin) may change any task in
     * the team; members only tasks they created or are assigned to. Tasks of a completed
     * project can't be changed at all.
     */
    private Task findChangeableTask(String key, UUID callerId) {
        Task task = findVisibleTask(key, callerId);
        if (!teamAccess.canManage(task.getTeamId(), callerId) && !task.belongsTo(callerId)) {
            throw new ForbiddenException("Members can only change tasks they created or are assigned to");
        }
        ProjectService.requireActive(task.getProject());
        return task;
    }

    /** The task if the caller may see its team; otherwise 404. Also used by other features (e.g. comments). */
    public Task findVisibleTask(String key, UUID callerId) {
        return findByKey(key)
                .filter(task -> teamAccess.canView(task.getTeamId(), callerId))
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    /** "PAY-12" (or "pay-12") means project code PAY, number 12. Anything else matches nothing. */
    private Optional<Task> findByKey(String key) {
        int dash = key.lastIndexOf('-');
        if (dash < 1) {
            return Optional.empty();
        }
        String code = key.substring(0, dash).toUpperCase(Locale.ROOT);
        try {
            return taskRepository.findByKey(code, Integer.parseInt(key.substring(dash + 1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Every assignee must be a member of the task's team (so never the admin). */
    private Set<User> findAssignees(UUID teamId, List<UUID> assigneeIds) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> wanted = new HashSet<>(assigneeIds);
        Set<User> members = teamMemberService.findMembers(teamId, wanted);
        if (members.size() != wanted.size()) {
            throw new BadRequestException("Assignees must be members of the team");
        }
        return members;
    }
}
