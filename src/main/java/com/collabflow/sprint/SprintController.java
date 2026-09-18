package com.collabflow.sprint;

import java.util.List;
import java.util.UUID;

import com.collabflow.shared.CurrentUser;
import com.collabflow.sprint.dto.SprintRequest;
import com.collabflow.sprint.dto.SprintResponse;
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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping("/teams/{teamId}/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse createSprint(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                                       @Valid @RequestBody SprintRequest request) {
        return sprintService.createSprint(CurrentUser.id(jwt), teamId, request);
    }

    /** {@code ?status=PLANNED|ACTIVE|COMPLETED} to filter; leave it out for all. */
    @GetMapping("/teams/{teamId}/sprints")
    public List<SprintResponse> listSprints(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                                            @RequestParam(required = false) SprintStatus status) {
        return sprintService.listSprints(CurrentUser.id(jwt), teamId, status);
    }

    @GetMapping("/sprints/{sprintId}")
    public SprintResponse getSprint(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sprintId) {
        return sprintService.getSprint(CurrentUser.id(jwt), sprintId);
    }

    @PutMapping("/sprints/{sprintId}")
    public SprintResponse updateSprint(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sprintId,
                                       @Valid @RequestBody SprintRequest request) {
        return sprintService.updateSprint(CurrentUser.id(jwt), sprintId, request);
    }

    @PostMapping("/sprints/{sprintId}/start")
    public SprintResponse startSprint(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sprintId) {
        return sprintService.startSprint(CurrentUser.id(jwt), sprintId);
    }

    @PostMapping("/sprints/{sprintId}/complete")
    public SprintResponse completeSprint(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sprintId) {
        return sprintService.completeSprint(CurrentUser.id(jwt), sprintId);
    }
}
