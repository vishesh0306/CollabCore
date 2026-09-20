package com.collabflow.sprint;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.sprint.dto.SprintRequest;
import com.collabflow.sprint.dto.SprintResponse;
import com.collabflow.team.Team;
import com.collabflow.team.TeamAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A team's sprints. Everyone in the team can see them; the team's managers (and the admin)
 * plan, start and complete them. Only one sprint per team can be active at a time.
 */
@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TeamAccess teamAccess;
    private final ApplicationEventPublisher events;

    @Transactional
    public SprintResponse createSprint(UUID callerId, UUID teamId, SprintRequest request) {
        Team team = teamAccess.requireManager(teamId, callerId);
        requireValidDates(request.startDate(), request.endDate());
        Sprint sprint = sprintRepository.saveAndFlush(
                new Sprint(team, request.name(), request.target(), request.startDate(), request.endDate()));
        return SprintResponse.from(sprint);
    }

    /** A team's sprints, newest first; optionally only one status. */
    @Transactional(readOnly = true)
    public List<SprintResponse> listSprints(UUID callerId, UUID teamId, SprintStatus status) {
        teamAccess.requireVisible(teamId, callerId);
        List<Sprint> sprints = status == null
                ? sprintRepository.findByTeamIdOrderByStartDateDesc(teamId)
                : sprintRepository.findByTeamIdAndStatusOrderByStartDateDesc(teamId, status);
        return sprints.stream().map(SprintResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprint(UUID callerId, UUID sprintId) {
        return SprintResponse.from(findVisibleSprint(sprintId, callerId));
    }

    @Transactional
    public SprintResponse updateSprint(UUID callerId, UUID sprintId, SprintRequest request) {
        Sprint sprint = findManageableSprint(sprintId, callerId);
        if (!sprint.isOpen()) {
            throw new ConflictException("A completed sprint can't be changed");
        }
        requireValidDates(request.startDate(), request.endDate());
        sprint.update(request.name(), request.target(), request.startDate(), request.endDate());
        return SprintResponse.from(sprint);
    }

    @Transactional
    public SprintResponse startSprint(UUID callerId, UUID sprintId) {
        Sprint sprint = findManageableSprint(sprintId, callerId);
        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new ConflictException("Only a planned sprint can be started");
        }
        UUID teamId = sprint.getTeam().getId();
        if (sprintRepository.existsByTeamIdAndStatus(teamId, SprintStatus.ACTIVE)) {
            throw anotherSprintIsActive();
        }
        sprint.start();
        try {
            // Two managers starting different sprints at the same moment both pass the check
            // above; the database's "one active sprint per team" index stops the second one here.
            sprintRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw anotherSprintIsActive();
        }
        events.publishEvent(new SprintStartedEvent(sprint.getId(), teamId, sprint.getName(), callerId));
        return SprintResponse.from(sprint);
    }

    @Transactional
    public SprintResponse completeSprint(UUID callerId, UUID sprintId) {
        Sprint sprint = findManageableSprint(sprintId, callerId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ConflictException("Only the active sprint can be completed");
        }
        sprint.complete();
        // Listeners (e.g. tasks) react right away, inside this same transaction.
        events.publishEvent(new SprintCompletedEvent(sprint.getId(), sprint.getTeam().getId()));
        return SprintResponse.from(sprint);
    }

    /** The sprint if the caller may see its team; otherwise 404. Also used by other features. */
    public Sprint findVisibleSprint(UUID sprintId, UUID callerId) {
        return sprintRepository.findById(sprintId)
                .filter(sprint -> teamAccess.canView(sprint.getTeam().getId(), callerId))
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
    }

    /** For other features: a planned or active sprint of this team, e.g. to put a task into. */
    public Sprint findOpenSprintOfTeam(UUID sprintId, UUID teamId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .filter(found -> found.getTeam().getId().equals(teamId))
                .orElseThrow(() -> new BadRequestException("The sprint must belong to the task's team"));
        if (!sprint.isOpen()) {
            throw new BadRequestException("Tasks can only be added to a planned or active sprint");
        }
        return sprint;
    }

    private Sprint findManageableSprint(UUID sprintId, UUID callerId) {
        Sprint sprint = findVisibleSprint(sprintId, callerId);
        if (!teamAccess.canManage(sprint.getTeam().getId(), callerId)) {
            throw new ForbiddenException("Only the team's managers can do this");
        }
        return sprint;
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("The end date can't be before the start date");
        }
    }

    private static ConflictException anotherSprintIsActive() {
        return new ConflictException("Another sprint is already active in this team. Complete it first.");
    }
}
