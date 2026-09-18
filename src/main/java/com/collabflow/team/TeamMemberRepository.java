package com.collabflow.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    /** The teams a user belongs to, with the team data loaded in the same query. */
    @Query("select m from TeamMember m join fetch m.team where m.user.id = :userId order by m.team.name")
    List<TeamMember> findMembershipsWithTeam(@Param("userId") UUID userId);
}
