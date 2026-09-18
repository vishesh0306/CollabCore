package com.collabflow.team;

import java.util.List;
import java.util.UUID;

import com.collabflow.shared.CurrentUser;
import com.collabflow.team.dto.CreateTeamRequest;
import com.collabflow.team.dto.TeamResponse;
import com.collabflow.team.dto.TeamSummaryResponse;
import com.collabflow.team.dto.UpdateTeamRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse createTeam(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTeamRequest request) {
        return teamService.createTeam(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<TeamSummaryResponse> listTeams(@AuthenticationPrincipal Jwt jwt) {
        return teamService.listTeams(CurrentUser.id(jwt));
    }

    @GetMapping("/{teamId}")
    public TeamResponse getTeam(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId) {
        return teamService.getTeam(CurrentUser.id(jwt), teamId);
    }

    @PutMapping("/{teamId}")
    public TeamResponse updateTeam(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                                   @Valid @RequestBody UpdateTeamRequest request) {
        return teamService.updateTeam(CurrentUser.id(jwt), teamId, request);
    }
}
