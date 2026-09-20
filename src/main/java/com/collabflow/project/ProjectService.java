package com.collabflow.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.project.dto.CreateProjectRequest;
import com.collabflow.project.dto.ProjectResponse;
import com.collabflow.project.dto.UpdateProjectRequest;
import com.collabflow.shared.FieldChange;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.team.Team;
import com.collabflow.team.TeamAccess;
import com.collabflow.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects inside a team. Everyone in the team can see them; only the team's managers
 * (and the admin) can create or change them. Permissions come from TeamAccess.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamAccess teamAccess;
    private final TeamMemberService teamMemberService;
    private final ApplicationEventPublisher events;

    @Transactional
    public ProjectResponse createProject(UUID callerId, UUID teamId, CreateProjectRequest request) {
        Team team = teamAccess.requireManager(teamId, callerId);
        if (projectRepository.existsByCode(request.code())) {
            throw codeTaken(request.code());
        }
        User lead = findLead(teamId, request.leadUserId());

        Project project = projectRepository.save(
                new Project(team, request.code(), request.name(), request.description(), lead));
        try {
            projectRepository.flush(); // INSERT now: fills createdAt and catches a code taken at the same moment
        } catch (DataIntegrityViolationException e) {
            throw codeTaken(request.code());
        }
        events.publishEvent(new ProjectEvents.Created(project.getId(), teamId, project.getCode(), callerId,
                List.of(FieldChange.set("name", project.getName()),
                        FieldChange.set("lead", lead.getName()))));
        return ProjectResponse.from(project);
    }

    /** A team's projects, optionally only ACTIVE or only COMPLETED ones. */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(UUID callerId, UUID teamId, ProjectStatus status) {
        teamAccess.requireVisible(teamId, callerId);
        List<Project> projects = status == null
                ? projectRepository.findByTeamWithLead(teamId)
                : projectRepository.findByTeamAndStatusWithLead(teamId, status);
        return projects.stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID callerId, UUID projectId) {
        return ProjectResponse.from(findVisibleProject(projectId, callerId));
    }

    @Transactional
    public ProjectResponse updateProject(UUID callerId, UUID projectId, UpdateProjectRequest request) {
        Project project = findManageableProject(projectId, callerId);
        requireActive(project);
        User lead = findLead(project.getTeam().getId(), request.leadUserId());
        List<FieldChange> changes = new ArrayList<>();
        addIfChanged(changes, "name", project.getName(), request.name());
        addIfChanged(changes, "description", project.getDescription(), request.description());
        addIfChanged(changes, "lead", project.getLead().getName(), lead.getName());

        project.update(request.name(), request.description(), lead);
        if (!changes.isEmpty()) {
            events.publishEvent(new ProjectEvents.Updated(project.getId(), project.getTeam().getId(),
                    project.getCode(), callerId, changes));
        }
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse completeProject(UUID callerId, UUID projectId) {
        Project project = findManageableProject(projectId, callerId);
        if (project.isCompleted()) {
            throw new ConflictException("This project is already completed");
        }
        project.complete();
        events.publishEvent(new ProjectEvents.Completed(project.getId(), project.getTeam().getId(),
                project.getCode(), callerId));
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse reopenProject(UUID callerId, UUID projectId) {
        Project project = findManageableProject(projectId, callerId);
        if (!project.isCompleted()) {
            throw new ConflictException("This project is already active");
        }
        project.reopen();
        events.publishEvent(new ProjectEvents.Reopened(project.getId(), project.getTeam().getId(),
                project.getCode(), callerId));
        return ProjectResponse.from(project);
    }

    private static void addIfChanged(List<FieldChange> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(FieldChange.of(field, before, after));
        }
    }

    /** A completed project (and its tasks) is read-only until a manager reopens it. */
    public static void requireActive(Project project) {
        if (project.isCompleted()) {
            throw new ConflictException("This project is completed. Reopen it to make changes.");
        }
    }

    /** The project if the caller may see its team; otherwise 404, so outsiders can't tell it exists. */
    public Project findVisibleProject(UUID projectId, UUID callerId) {
        return projectRepository.findById(projectId)
                .filter(project -> teamAccess.canView(project.getTeam().getId(), callerId))
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    /**
     * Like findVisibleProject, but also locks the project's row until the caller's transaction
     * ends. Used when creating a task, so task numbers are handed out one at a time.
     */
    public Project findVisibleProjectForUpdate(UUID projectId, UUID callerId) {
        return projectRepository.findForUpdateById(projectId)
                .filter(project -> teamAccess.canView(project.getTeam().getId(), callerId))
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private Project findManageableProject(UUID projectId, UUID callerId) {
        Project project = findVisibleProject(projectId, callerId);
        if (!teamAccess.canManage(project.getTeam().getId(), callerId)) {
            throw new ForbiddenException("Only the team's managers can do this");
        }
        return project;
    }

    private User findLead(UUID teamId, UUID userId) {
        return teamMemberService.findManager(teamId, userId)
                .orElseThrow(() -> new BadRequestException("The lead must be one of the team's managers"));
    }

    private static ConflictException codeTaken(String code) {
        return new ConflictException("The project code " + code + " is already used");
    }
}
