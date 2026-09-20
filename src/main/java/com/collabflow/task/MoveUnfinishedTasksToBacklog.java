package com.collabflow.task;

import java.util.List;

import com.collabflow.sprint.SprintCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * When a sprint is completed, its unfinished tasks go back to their project's backlog.
 * Finished (DONE) tasks stay with the sprint, as the record of what it achieved.
 *
 * <p>Runs immediately, inside the same transaction as completing the sprint.
 *
 * <p>The tasks are read before the bulk update so each one can be announced by name. These
 * moves carry no actor: the manager completed a sprint, they didn't touch forty tasks.
 */
@Component
@RequiredArgsConstructor
class MoveUnfinishedTasksToBacklog {

    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher events;

    @EventListener
    void on(SprintCompletedEvent event) {
        List<Task> moving = taskRepository.findInSprint(event.sprintId()).stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .toList();

        taskRepository.moveUnfinishedTasksToBacklog(event.sprintId());

        for (Task task : moving) {
            events.publishEvent(new TaskEvents.MovedToSprint(TaskRef.of(task), null,
                    event.sprintName(), "Backlog"));
        }
    }
}
