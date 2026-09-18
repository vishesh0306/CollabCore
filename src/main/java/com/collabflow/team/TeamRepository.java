package com.collabflow.team;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    boolean existsByNameIgnoreCase(String name);

    /** Is the name used by a team other than this one? (For renaming.) */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
