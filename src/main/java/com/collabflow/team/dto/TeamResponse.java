package com.collabflow.team.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.collabflow.team.Team;
import com.collabflow.team.TeamMember;

/** A team with its members (managers first). */
public record TeamResponse(UUID id, String name, String description, Instant createdAt, List<MemberResponse> members) {

    public static TeamResponse from(Team team, List<TeamMember> members) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedAt(),
                members.stream().map(MemberResponse::from).toList());
    }
}
