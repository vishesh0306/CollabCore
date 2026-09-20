package com.collabflow.task;

import com.collabflow.team.TeamEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * When someone is removed from a team, they stop being an assignee on that team's tasks
 * (their tasks in other teams stay as they are).
 *
 * <p>A plain @EventListener runs immediately, inside the same transaction as the removal,
 * so both changes are saved together or not at all.
 */
@Component
@RequiredArgsConstructor
class UnassignRemovedTeamMember {

    private final TaskRepository taskRepository;

    @EventListener
    void on(TeamEvents.MemberRemoved event) {
        taskRepository.removeAssigneeFromTeamTasks(event.teamId(), event.userId());
    }
}
