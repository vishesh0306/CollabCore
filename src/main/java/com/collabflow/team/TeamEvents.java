package com.collabflow.team;

import java.util.List;
import java.util.UUID;

import com.collabflow.shared.FieldChange;

/**
 * What happened to a team or to someone's place in it. Notifications, tasks and the audit log
 * all listen for these; the team module knows none of them.
 */
public final class TeamEvents {

    private TeamEvents() {
    }

    public record Created(UUID teamId, String teamName, UUID actorId, List<FieldChange> fields) {
    }

    public record Updated(UUID teamId, String teamName, UUID actorId, List<FieldChange> changes) {
    }

    /** Someone joined, whether at team creation or later. */
    public record MemberAdded(UUID teamId, String teamName, UUID actorId, UUID userId, TeamRole role) {
    }

    public record MemberRoleChanged(UUID teamId, UUID actorId, UUID userId, TeamRole from, TeamRole to) {
    }

    public record MemberRemoved(UUID teamId, UUID actorId, UUID userId) {
    }
}
