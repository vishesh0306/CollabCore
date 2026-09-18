package com.collabflow.team;

import java.util.Optional;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.identity.UserService;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.team.dto.AddMemberRequest;
import com.collabflow.team.dto.ChangeRoleRequest;
import com.collabflow.team.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Who is in a team and with which role. Only the team's managers (and the admin) may change it. */
@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository memberRepository;
    private final TeamAccess teamAccess;
    private final UserService userService;

    @Transactional
    public MemberResponse addMember(UUID callerId, UUID teamId, AddMemberRequest request) {
        Team team = teamAccess.requireManager(teamId, callerId);
        User user = findUserToAdd(request.email());
        if (memberRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw new ConflictException(user.getEmail() + " is already in this team");
        }
        try {
            return MemberResponse.from(memberRepository.saveAndFlush(new TeamMember(team, user, request.role())));
        } catch (DataIntegrityViolationException e) {
            // Someone added the same person at the same moment; the unique (team, user) rule stopped it.
            throw new ConflictException(user.getEmail() + " is already in this team");
        }
    }

    @Transactional
    public MemberResponse changeRole(UUID callerId, UUID teamId, UUID userId, ChangeRoleRequest request) {
        teamAccess.requireManager(teamId, callerId);
        TeamMember member = findMember(teamId, userId);
        if (member.getRole() == TeamRole.MANAGER && request.role() != TeamRole.MANAGER) {
            requireAnotherManager(teamId);
        }
        member.changeRole(request.role()); // saved automatically when the transaction commits
        return MemberResponse.from(member);
    }

    @Transactional
    public void removeMember(UUID callerId, UUID teamId, UUID userId) {
        teamAccess.requireManager(teamId, callerId);
        TeamMember member = findMember(teamId, userId);
        if (member.getRole() == TeamRole.MANAGER) {
            requireAnotherManager(teamId);
        }
        memberRepository.delete(member);
    }

    /** The user, if they are a manager of the team. Other features use this, e.g. to check a project lead. */
    @Transactional(readOnly = true)
    public Optional<User> findManager(UUID teamId, UUID userId) {
        return memberRepository.findByTeamIdAndUserId(teamId, userId)
                .filter(member -> member.getRole() == TeamRole.MANAGER)
                .map(TeamMember::getUser);
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

    private TeamMember findMember(UUID teamId, UUID userId) {
        return memberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException("This user is not in the team"));
    }

    /**
     * A team must always keep at least one manager.
     *
     * <p>Known gap: this is a plain "count, then change" check. If two managers demote each
     * other at the very same moment, both can see "2 managers" and both succeed, leaving none.
     * Closing that gap needs a database lock on the team.
     */
    private void requireAnotherManager(UUID teamId) {
        if (memberRepository.countByTeamIdAndRole(teamId, TeamRole.MANAGER) <= 1) {
            throw new ConflictException("A team must keep at least one manager");
        }
    }
}
