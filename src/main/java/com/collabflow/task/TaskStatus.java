package com.collabflow.task;

/** A task can move freely between these. Moving to DONE records when it was completed. */
public enum TaskStatus {
    TO_DO,
    IN_PROGRESS,
    IN_REVIEW,
    BLOCKED,
    DONE
}
