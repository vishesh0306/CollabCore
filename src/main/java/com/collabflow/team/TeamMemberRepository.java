package com.collabflow.team;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.collabflow.identity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    // "TeamId" in a method name means team.id: Spring Data follows the relationship.
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    long countByTeamIdAndRole(UUID teamId, TeamRole role);

    /**
     * A team's members with their user data loaded in the same query ("join fetch").
     * Without it, reading each member's name would run one extra query per member (N+1).
     * Sorting by role puts MANAGER before MEMBER (alphabetical).
     */
    @Query("select m from TeamMember m join fetch m.user where m.team.id = :teamId order by m.role, m.user.name")
    List<TeamMember> findMembersWithUser(@Param("teamId") UUID teamId);

    /** Just the ids of everyone in the team, e.g. to notify them all. */
    @Query("select m.user.id from TeamMember m where m.team.id = :teamId")
    List<UUID> findMemberIds(@Param("teamId") UUID teamId);

    /** Of the given users, the ones who are members (any role) of the team. */
    @Query("select m.user from TeamMember m where m.team.id = :teamId and m.user.id in :userIds")
    List<User> findMemberUsers(@Param("teamId") UUID teamId, @Param("userIds") Collection<UUID> userIds);

    /** The teams a user belongs to, with the team data loaded in the same query. */
    @Query("select m from TeamMember m join fetch m.team where m.user.id = :userId order by m.team.name")
    List<TeamMember> findMembershipsWithTeam(@Param("userId") UUID userId);
}
