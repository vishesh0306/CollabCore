package com.collabflow.task;

import java.util.UUID;

/**
 * Which task an event is about, as plain data: enough for a listener to write a message or a log
 * entry without loading the task again (and it still works once the task is deleted).
 */
public record TaskRef(UUID id, UUID teamId, UUID projectId, String key, String title) {

    public static TaskRef of(Task task) {
        return new TaskRef(task.getId(), task.getTeamId(), task.getProject().getId(), task.getKey(),
                task.getTitle());
    }
}
