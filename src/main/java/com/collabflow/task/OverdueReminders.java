package com.collabflow.task;

import java.util.List;

import com.collabflow.shared.CompanyTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds tasks whose expected date has passed and that aren't done, and announces them once.
 *
 * <p>"Once" is remembered on the task itself, in overdue_reminded_for. It holds a date rather
 * than a yes/no, so if a manager moves the expected date and that date passes too, a fresh
 * reminder goes out.
 */
@Service
@RequiredArgsConstructor
public class OverdueReminders {

    /** Small batches keep each transaction (and each row lock) short. */
    static final int BATCH_SIZE = 50;

    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher events;

    /** Reminds about the next batch of overdue tasks and returns how many there were. */
    @Transactional
    public int remindNextBatch() {
        List<Task> overdue = taskRepository.claimOverdueTasks(CompanyTime.today(), BATCH_SIZE);
        for (Task task : overdue) {
            task.markOverdueReminded();
            events.publishEvent(new TaskEvents.Overdue(TaskRef.of(task), task.getExpectedDate(),
                    task.assigneeIds()));
        }
        return overdue.size();
    }
}
