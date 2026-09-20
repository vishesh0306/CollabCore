package com.collabflow.task;

/** A task can move freely between these. Moving to DONE records when it was completed. */
public enum TaskStatus {

    TO_DO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    BLOCKED("Blocked"),
    DONE("Done");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    /** How the status is written in messages people read. */
    public String label() {
        return label;
    }
}
