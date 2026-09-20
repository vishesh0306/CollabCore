package com.collabflow.audit;

import java.util.List;

import com.collabflow.shared.FieldChange;
import com.collabflow.team.TeamEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The log for teams and for who belongs to them.
 *
 * <p>A membership entry is filed under the person (so you can follow one person's place in a
 * team over time) and carries the team id, so it also shows up in the team's activity.
 */
@Component
@RequiredArgsConstructor
class TeamAudit {

    private final AuditService audit;
    private final AuditNames names;

    @EventListener
    void onCreated(TeamEvents.Created event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.TEAM, event.teamId(),
                event.teamName(), AuditAction.CREATED, event.fields());
    }

    @EventListener
    void onUpdated(TeamEvents.Updated event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.TEAM, event.teamId(),
                event.teamName(), AuditAction.UPDATED, event.changes());
    }

    @EventListener
    void onMemberAdded(TeamEvents.MemberAdded event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.TEAM_MEMBER, event.userId(),
                names.of(event.userId()), AuditAction.MEMBER_ADDED,
                List.of(FieldChange.set("role", event.role())));
    }

    @EventListener
    void onRoleChanged(TeamEvents.MemberRoleChanged event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.TEAM_MEMBER, event.userId(),
                names.of(event.userId()), AuditAction.ROLE_CHANGED,
                List.of(FieldChange.of("role", event.from(), event.to())));
    }

    @EventListener
    void onMemberRemoved(TeamEvents.MemberRemoved event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.TEAM_MEMBER, event.userId(),
                names.of(event.userId()), AuditAction.MEMBER_REMOVED, List.of());
    }
}
