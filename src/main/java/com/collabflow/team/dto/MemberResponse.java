package com.collabflow.team.dto;

import java.time.Instant;
import java.util.UUID;

import com.collabflow.team.TeamMember;
import com.collabflow.team.TeamRole;

public record MemberResponse(UUID userId, String name, String email, TeamRole role, Instant joinedAt) {

    public static MemberResponse from(TeamMember member) {
        return new MemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt());
    }
}
