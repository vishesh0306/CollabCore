package com.collabflow.team;

import java.util.UUID;

/**
 * Announced when someone is removed from a team. The team module doesn't know which other
 * features care; they listen for this event (e.g. tasks unassign the person).
 */
public record TeamMemberRemovedEvent(UUID teamId, UUID userId) {
}
