package com.collabflow.project;

import java.util.List;
import java.util.UUID;

import com.collabflow.shared.FieldChange;

/**
 * What happened to a project. The audit log listens for these inside the same transaction;
 * the project module doesn't know it exists.
 */
public final class ProjectEvents {

    private ProjectEvents() {
    }

    public record Created(UUID projectId, UUID teamId, String code, UUID actorId, List<FieldChange> fields) {
    }

    public record Updated(UUID projectId, UUID teamId, String code, UUID actorId, List<FieldChange> changes) {
    }

    public record Completed(UUID projectId, UUID teamId, String code, UUID actorId) {
    }

    public record Reopened(UUID projectId, UUID teamId, String code, UUID actorId) {
    }
}
