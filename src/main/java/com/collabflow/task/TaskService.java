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
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.task.dto.CreateTaskRequest;
import com.collabflow.task.dto.TaskResponse;
import com.collabflow.team.TeamAccess;
import com.collabflow.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
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
