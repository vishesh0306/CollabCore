package com.collabflow.sprint;

import java.util.List;
import java.util.UUID;

import com.collabflow.shared.FieldChange;

/**
 * Planning changes to a sprint, for the audit log. Starting and completing a sprint have their
 * own events, because other modules (notifications, tasks) act on those two.
 */
public final class SprintEvents {

    private SprintEvents() {
    }

    public record Created(UUID sprintId, UUID teamId, String name, UUID actorId, List<FieldChange> fields) {
    }

    public record Updated(UUID sprintId, UUID teamId, String name, UUID actorId, List<FieldChange> changes) {
    }
}
