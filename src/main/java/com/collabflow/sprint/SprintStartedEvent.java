package com.collabflow.sprint;

import java.util.UUID;

/** Announced when a team's sprint starts. Everyone in the team is told about it. */
public record SprintStartedEvent(UUID sprintId, UUID teamId, String sprintName, UUID actorId) {
}
