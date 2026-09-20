package com.collabflow.sprint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.collabflow.shared.FieldChange;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.sprint.dto.CompleteSprintRequest;
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
        events.publishEvent(new SprintEvents.Created(sprint.getId(), teamId, sprint.getName(), callerId,
                List.of(FieldChange.set("target", sprint.getTarget()),
                        FieldChange.set("startDate", sprint.getStartDate()),
                        FieldChange.set("endDate", sprint.getEndDate()))));
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
        List<FieldChange> changes = new ArrayList<>();
        addIfChanged(changes, "name", sprint.getName(), request.name());
        addIfChanged(changes, "target", sprint.getTarget(), request.target());
        addIfChanged(changes, "startDate", sprint.getStartDate(), request.startDate());
        addIfChanged(changes, "endDate", sprint.getEndDate(), request.endDate());

        sprint.update(request.name(), request.target(), request.startDate(), request.endDate());
        if (!changes.isEmpty()) {
            events.publishEvent(new SprintEvents.Updated(sprint.getId(), sprint.getTeam().getId(),
                    sprint.getName(), callerId, changes));
        }
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

    /**
     * Completes a sprint and, if asked, carries its unfinished work over: every task of this
     * sprint that isn't Done is tagged into the named sprint as well. The old tag stays, so this
     * sprint still shows what was in it. Tasks that are Done are left alone.
     */
    @Transactional
    public SprintResponse completeSprint(UUID callerId, UUID sprintId, CompleteSprintRequest request) {
        Sprint sprint = findManageableSprint(sprintId, callerId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ConflictException("Only the active sprint can be completed");
        }
        UUID carryOverTo = request == null ? null : request.carryOverToSprintId();
        if (carryOverTo != null) {
            if (carryOverTo.equals(sprintId)) {
                throw new BadRequestException("A sprint can't carry work over into itself");
            }
            findSprintOfTeam(carryOverTo, sprint.getTeam().getId());
        }
        sprint.complete();
        // Listeners (the task module carries the work over) run inside this same transaction.
        events.publishEvent(new SprintCompletedEvent(sprint.getId(), sprint.getTeam().getId(),
                sprint.getName(), callerId, carryOverTo));
        return SprintResponse.from(sprint);
    }

    /** The sprint if the caller may see its team; otherwise 404. Also used by other features. */
    public Sprint findVisibleSprint(UUID sprintId, UUID callerId) {
        return sprintRepository.findById(sprintId)
                .filter(sprint -> teamAccess.canView(sprint.getTeam().getId(), callerId))
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
    }

    /**
     * For other features: a sprint of this team, to tag a task into. Completed sprints count too
     * — a finished sprint stays editable, so a task forgotten at the time can still be added.
     */
    public Sprint findSprintOfTeam(UUID sprintId, UUID teamId) {
        return sprintRepository.findById(sprintId)
                .filter(found -> found.getTeam().getId().equals(teamId))
                .orElseThrow(() -> new BadRequestException("The sprint must belong to the task's team"));
    }

    private Sprint findManageableSprint(UUID sprintId, UUID callerId) {
        Sprint sprint = findVisibleSprint(sprintId, callerId);
        if (!teamAccess.canManage(sprint.getTeam().getId(), callerId)) {
            throw new ForbiddenException("Only the team's managers can do this");
        }
        return sprint;
    }

    private static void addIfChanged(List<FieldChange> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(FieldChange.of(field, before, after));
        }
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
