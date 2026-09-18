package com.collabflow.project;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByCode(String code);

    /** A team's projects with each lead loaded in the same query (the list shows lead names). */
    @Query("select p from Project p join fetch p.lead where p.team.id = :teamId order by p.name")
    List<Project> findByTeamWithLead(@Param("teamId") UUID teamId);

    @Query("select p from Project p join fetch p.lead where p.team.id = :teamId and p.status = :status order by p.name")
    List<Project> findByTeamAndStatusWithLead(@Param("teamId") UUID teamId, @Param("status") ProjectStatus status);
}
