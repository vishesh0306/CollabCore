package com.collabflow.team;

import java.util.UUID;

/** Announced when someone joins a team, whether at team creation or later. */
public record TeamMemberAddedEvent(UUID teamId, String teamName, UUID actorId, UUID userId) {
}
