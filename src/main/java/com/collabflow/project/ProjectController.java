package com.collabflow.project;

import java.util.List;
import java.util.UUID;

import com.collabflow.project.dto.CreateProjectRequest;
import com.collabflow.project.dto.ProjectResponse;
import com.collabflow.project.dto.UpdateProjectRequest;
import com.collabflow.shared.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Projects are created and listed under their team, and then used by their own id. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/teams/{teamId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                                         @Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(CurrentUser.id(jwt), teamId, request);
    }

    /** {@code ?status=ACTIVE} or {@code ?status=COMPLETED} to filter; leave it out for all. */
    @GetMapping("/teams/{teamId}/projects")
    public List<ProjectResponse> listProjects(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                                              @RequestParam(required = false) ProjectStatus status) {
        return projectService.listProjects(CurrentUser.id(jwt), teamId, status);
    }

    @GetMapping("/projects/{projectId}")
    public ProjectResponse getProject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return projectService.getProject(CurrentUser.id(jwt), projectId);
    }

    @PutMapping("/projects/{projectId}")
    public ProjectResponse updateProject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
                                         @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.updateProject(CurrentUser.id(jwt), projectId, request);
    }

    @PostMapping("/projects/{projectId}/complete")
    public ProjectResponse completeProject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return projectService.completeProject(CurrentUser.id(jwt), projectId);
    }

    @PostMapping("/projects/{projectId}/reopen")
    public ProjectResponse reopenProject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return projectService.reopenProject(CurrentUser.id(jwt), projectId);
    }
}
