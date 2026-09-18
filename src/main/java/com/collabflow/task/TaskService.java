package com.collabflow.task;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.identity.UserService;
import com.collabflow.project.Project;
import com.collabflow.project.ProjectService;
import com.collabflow.shared.PageResponse;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.task.dto.ChangeStatusRequest;
import com.collabflow.task.dto.CreateTaskRequest;
import com.collabflow.task.dto.ReplaceAssigneesRequest;
import com.collabflow.task.dto.TaskResponse;
import com.collabflow.task.dto.UpdateTaskRequest;
import com.collabflow.team.TeamAccess;
import com.collabflow.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
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
    private final TeamAccess teamAccess;
    private final TeamMemberService teamMemberService;
    private final UserService userService;

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
        task.updateDetails(request.title(), request.description(), request.expectedDate());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(UUID callerId, String key, ChangeStatusRequest request) {
        Task task = findChangeableTask(key, callerId);
        task.changeStatus(request.status());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse replaceAssignees(UUID callerId, String key, ReplaceAssigneesRequest request) {
        Task task = findChangeableTask(key, callerId);
        task.replaceAssignees(findAssignees(task.getTeamId(), request.assigneeIds()));
        return TaskResponse.from(task);
    }

    /** Soft delete: the row stays (and keeps its number), but the task is hidden from now on. */
    @Transactional
    public void deleteTask(UUID callerId, String key) {
        findChangeableTask(key, callerId).delete();
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

    /** The task if the caller may see its team; otherwise 404. */
    private Task findVisibleTask(String key, UUID callerId) {
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
