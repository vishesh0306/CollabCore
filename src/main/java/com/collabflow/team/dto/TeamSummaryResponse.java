package com.collabflow.team.dto;

import java.util.UUID;

import com.collabflow.team.Team;
import com.collabflow.team.TeamRole;

/** One row in "my teams". {@code myRole} is null for the admin, who isn't a member of any team. */
public record TeamSummaryResponse(UUID id, String name, String description, TeamRole myRole) {

    public static TeamSummaryResponse from(Team team, TeamRole myRole) {
        return new TeamSummaryResponse(team.getId(), team.getName(), team.getDescription(), myRole);
    }
}
