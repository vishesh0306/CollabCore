package com.collabflow.task;

import com.collabflow.sprint.SprintCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * When a sprint is completed, its unfinished tasks go back to their project's backlog.
 * Finished (DONE) tasks stay with the sprint, as the record of what it achieved.
 *
 * <p>Runs immediately, inside the same transaction as completing the sprint.
 */
@Component
@RequiredArgsConstructor
class MoveUnfinishedTasksToBacklog {

    private final TaskRepository taskRepository;

    @EventListener
    void on(SprintCompletedEvent event) {
        taskRepository.moveUnfinishedTasksToBacklog(event.sprintId());
    }
}
