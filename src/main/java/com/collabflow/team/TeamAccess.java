package com.collabflow.team;

import java.util.UUID;

import com.collabflow.identity.UserService;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The one place that decides who may see or manage a team. Every feature inside a team
 * (projects, sprints, tasks) asks this class instead of repeating the rules.
 *
 * <ul>
 *   <li>Admin: may see and manage every team.</li>
 *   <li>Manager of the team: may see and manage it.</li>
 *   <li>Member of the team: may see it.</li>
 *   <li>Anyone else: gets 404, exactly as if the team didn't exist, so they can't even
 *       find out which teams exist.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TeamAccess {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final UserService userService;

    /** May the caller see this team and everything in it? */
    public boolean canView(UUID teamId, UUID callerId) {
        return userService.isAdmin(callerId) || memberRepository.existsByTeamIdAndUserId(teamId, callerId);
    }

    /** May the caller manage this team and everything in it? */
    public boolean canManage(UUID teamId, UUID callerId) {
        return userService.isAdmin(callerId) || memberRepository.findByTeamIdAndUserId(teamId, callerId)
                .map(member -> member.getRole() == TeamRole.MANAGER)
                .orElse(false);
    }

    /** Returns the team if the caller may see it; otherwise 404. */
    public Team requireVisible(UUID teamId, UUID callerId) {
        Team team = findTeam(teamId);
        if (!canView(teamId, callerId)) {
            throw teamNotFound();
        }
        return team;
    }

    /** Returns the team if the caller may manage it; members get 403, outsiders 404. */
    public Team requireManager(UUID teamId, UUID callerId) {
        Team team = requireVisible(teamId, callerId);
        if (!canManage(teamId, callerId)) {
            throw new ForbiddenException("Only the team's managers can do this");
        }
        return team;
    }

    public void requireAdmin(UUID callerId) {
        if (!userService.isAdmin(callerId)) {
            throw new ForbiddenException("Only the admin can do this");
        }
    }

    private Team findTeam(UUID teamId) {
        return teamRepository.findById(teamId).orElseThrow(TeamAccess::teamNotFound);
    }

    private static NotFoundException teamNotFound() {
        return new NotFoundException("Team not found");
    }
}
