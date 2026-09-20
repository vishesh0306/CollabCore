package com.collabflow.task;

import com.collabflow.sprint.Sprint;
import com.collabflow.sprint.SprintCompletedEvent;
import com.collabflow.sprint.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * When a sprint is completed with a sprint to carry over into, every unfinished task of it is
 * tagged into that sprint as well. Tasks keep the tag of the sprint that just ended, so the
 * finished sprint still shows everything that was in it, done or not.
 *
 * <p>Runs immediately, inside the same transaction as completing the sprint: the sprint is never
 * completed without the carry-over, and the carry-over never happens without the sprint closing.
 */
@Component
@RequiredArgsConstructor
class CarryOverUnfinishedTasks {

    private final TaskRepository taskRepository;
    private final SprintService sprintService;
    private final ApplicationEventPublisher events;

    @EventListener
    void on(SprintCompletedEvent event) {
        if (event.carryOverToSprintId() == null) {
            return;
        }
        Sprint target = sprintService.findSprintOfTeam(event.carryOverToSprintId(), event.teamId());
        for (Task task : taskRepository.findUnfinishedInSprint(event.sprintId())) {
            if (task.addToSprint(target)) {
                // The manager asked for this, so it is their name on it, not the system's.
                events.publishEvent(new TaskEvents.SprintTagged(TaskRef.of(task), event.actorId(),
                        target.getId(), target.getName()));
            }
        }
    }
}
