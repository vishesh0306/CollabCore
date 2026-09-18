package com.collabflow.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * The optional filters of the task list. Each filter is a small Specification (one WHERE
 * condition), and only the filters that were actually sent are combined with AND.
 */
public record TaskFilters(
        UUID projectId,
        TaskStatus status,
        UUID assigneeId,
        LocalDate expectedBefore,
        LocalDate expectedAfter) {

    Specification<Task> toSpecification(UUID teamId) {
        List<Specification<Task>> conditions = new ArrayList<>();
        conditions.add((task, query, cb) -> cb.equal(task.get("teamId"), teamId));

        if (projectId != null) {
            conditions.add((task, query, cb) -> cb.equal(task.get("project").get("id"), projectId));
        }
        if (status != null) {
            conditions.add((task, query, cb) -> cb.equal(task.get("status"), status));
        }
        if (assigneeId != null) {
            conditions.add((task, query, cb) -> {
                Join<Object, Object> assignee = task.join("assignees");
                return cb.equal(assignee.get("id"), assigneeId);
            });
        }
        if (expectedBefore != null) {
            conditions.add((task, query, cb) -> cb.lessThanOrEqualTo(task.get("expectedDate"), expectedBefore));
        }
        if (expectedAfter != null) {
            conditions.add((task, query, cb) -> cb.greaterThanOrEqualTo(task.get("expectedDate"), expectedAfter));
        }
        return Specification.allOf(conditions);
    }
}
