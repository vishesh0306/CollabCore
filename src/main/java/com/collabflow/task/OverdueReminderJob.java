package com.collabflow.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the overdue reminders in the background, a batch at a time.
 *
 * <p>Nobody triggers a reminder, so nothing in the API can send it: a clock has to. The job
 * keeps asking for batches until there is nothing left, with a cap so one run can't spin
 * forever. Tests switch it off with collabflow.reminders.enabled and call the batch directly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "collabflow.reminders.enabled", matchIfMissing = true)
class OverdueReminderJob {

    private static final int MAX_BATCHES_PER_RUN = 20;

    private final OverdueReminders reminders;

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
    void remindAboutOverdueTasks() {
        int total = 0;
        for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
            int reminded = reminders.remindNextBatch();
            total += reminded;
            if (reminded < OverdueReminders.BATCH_SIZE) {
                break;
            }
        }
        if (total > 0) {
            log.info("Reminded the assignees of {} overdue tasks", total);
        }
    }
}
