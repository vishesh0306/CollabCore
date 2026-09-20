package com.collabflow.team;

import java.util.List;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.identity.UserService;
import com.collabflow.shared.FieldChange;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.team.dto.CreateTeamRequest;
import com.collabflow.team.dto.TeamResponse;
import com.collabflow.team.dto.TeamSummaryResponse;
import com.collabflow.team.dto.UpdateTeamRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creating, listing, viewing and renaming teams.
 *
 * <p>These methods return response objects, not entities. The members' user data is loaded
 * lazily, and the database session closes when the transaction ends; reading it after that
 * (e.g. in the controller) would fail with a LazyInitializationException. So everything the
 * response needs is read here, while the transaction is still open.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamAccess teamAccess;
    private final TeamMemberService memberService;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Transactional
    public TeamResponse createTeam(UUID callerId, CreateTeamRequest request) {
        teamAccess.requireAdmin(callerId);
        if (teamRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A team with this name already exists");
        }
        User manager = memberService.findUserToAdd(request.managerEmail());

        Team team = teamRepository.save(new Team(request.name(), request.description()));
        TeamMember firstManager = memberRepository.save(new TeamMember(team, manager, TeamRole.MANAGER));
        events.publishEvent(new TeamEvents.Created(team.getId(), team.getName(), callerId,
                List.of(FieldChange.set("name", team.getName()))));
        events.publishEvent(new TeamEvents.MemberAdded(team.getId(), team.getName(), callerId,
                manager.getId(), TeamRole.MANAGER));
        // Run both INSERTs now. The @CreationTimestamp values are only filled in when the INSERT
        // runs, so without this the response would show createdAt and joinedAt as null.
        flushOrConflict();
        return TeamResponse.from(team, List.of(firstManager));
    }

    /** The admin sees every team; everyone else sees only the teams they belong to. */
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeams(UUID callerId) {
        if (userService.isAdmin(callerId)) {
            return teamRepository.findAll(Sort.by("name")).stream()
                    .map(team -> TeamSummaryResponse.from(team, null))
                    .toList();
        }
        return memberRepository.findMembershipsWithTeam(callerId).stream()
                .map(membership -> TeamSummaryResponse.from(membership.getTeam(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID callerId, UUID teamId) {
        Team team = teamAccess.requireVisible(teamId, callerId);
        return TeamResponse.from(team, memberRepository.findMembersWithUser(teamId));
    }

    @Transactional
    public TeamResponse updateTeam(UUID callerId, UUID teamId, UpdateTeamRequest request) {
        teamAccess.requireAdmin(callerId);
        Team team = teamAccess.requireVisible(teamId, callerId);
        if (teamRepository.existsByNameIgnoreCaseAndIdNot(request.name(), teamId)) {
            throw new ConflictException("A team with this name already exists");
        }
        team.update(request.name(), request.description());
        flushOrConflict();
        return TeamResponse.from(team, memberRepository.findMembersWithUser(teamId));
    }

    /** Writes pending changes now, so a name taken at the same moment by someone else is caught here. */
    private void flushOrConflict() {
        try {
            teamRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A team with this name already exists");
        }
    }
}
