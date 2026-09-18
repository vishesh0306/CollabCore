package com.collabflow.sprint;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    boolean existsByTeamIdAndStatus(UUID teamId, SprintStatus status);

    List<Sprint> findByTeamIdOrderByStartDateDesc(UUID teamId);

    List<Sprint> findByTeamIdAndStatusOrderByStartDateDesc(UUID teamId, SprintStatus status);
}
