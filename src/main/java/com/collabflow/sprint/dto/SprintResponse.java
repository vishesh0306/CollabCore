package com.collabflow.sprint.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.collabflow.sprint.Sprint;
import com.collabflow.sprint.SprintStatus;

public record SprintResponse(
        UUID id,
        UUID teamId,
        String name,
        String target,
        LocalDate startDate,
        LocalDate endDate,
        SprintStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {

    public static SprintResponse from(Sprint sprint) {
        return new SprintResponse(
                sprint.getId(),
                sprint.getTeam().getId(),
                sprint.getName(),
                sprint.getTarget(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getStatus(),
                sprint.getStartedAt(),
                sprint.getCompletedAt(),
                sprint.getCreatedAt());
    }
}
