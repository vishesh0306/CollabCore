package com.collabflow.team;

import java.util.List;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.identity.UserService;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.team.dto.CreateTeamRequest;
import com.collabflow.team.dto.TeamResponse;
import com.collabflow.team.dto.TeamSummaryResponse;
import com.collabflow.team.dto.UpdateTeamRequest;
import lombok.RequiredArgsConstructor;
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
    private final UserService userService;

    @Transactional
    public TeamResponse createTeam(UUID callerId, CreateTeamRequest request) {
        teamAccess.requireAdmin(callerId);
        if (teamRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A team with this name already exists");
        }
        User manager = findUserToAdd(request.managerEmail());

        Team team = teamRepository.save(new Team(request.name(), request.description()));
        TeamMember firstManager = memberRepository.save(new TeamMember(team, manager, TeamRole.MANAGER));
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
        return TeamResponse.from(team, memberRepository.findMembersWithUser(teamId));
    }

    /** A registered user who can join a team. The admin oversees all teams but never joins one. */
    User findUserToAdd(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No user is registered with the email " + email));
        if (user.isAdmin()) {
            throw new BadRequestException("The admin can't be added to a team");
        }
        return user;
    }
}
