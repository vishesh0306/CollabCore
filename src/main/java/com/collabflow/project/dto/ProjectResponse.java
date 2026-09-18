package com.collabflow.project.dto;

import java.time.Instant;
import java.util.UUID;

import com.collabflow.project.Project;
import com.collabflow.project.ProjectStatus;

public record ProjectResponse(
        UUID id,
        UUID teamId,
        String code,
        String name,
        String description,
        ProjectStatus status,
        UUID leadUserId,
        String leadName,
        Instant createdAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTeam().getId(),
                project.getCode(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getLead().getId(),
                project.getLead().getName(),
                project.getCreatedAt());
    }
}
