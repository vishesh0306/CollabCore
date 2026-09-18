package com.collabflow.project;

import java.util.List;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.project.dto.CreateProjectRequest;
import com.collabflow.project.dto.ProjectResponse;
import com.collabflow.project.dto.UpdateProjectRequest;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.team.Team;
import com.collabflow.team.TeamAccess;
import com.collabflow.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
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
        User lead = findLead(project.getTeam().getId(), request.leadUserId());
        project.update(request.name(), request.description(), lead);
        return ProjectResponse.from(project);
    }

    /** 404 unless the caller may see the project's team, so outsiders can't tell it exists. */
    private Project findVisibleProject(UUID projectId, UUID callerId) {
        return projectRepository.findById(projectId)
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
