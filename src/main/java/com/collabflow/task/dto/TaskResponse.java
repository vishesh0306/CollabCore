package com.collabflow.task.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.task.Task;
import com.collabflow.task.TaskStatus;

public record TaskResponse(
        UUID id,
        String key,
        UUID projectId,
        List<SprintTag> sprints,
        String title,
        String description,
        TaskStatus status,
        LocalDate expectedDate,
        Instant completedAt,
        Person createdBy,
        List<Person> assignees,
        Instant createdAt) {

    public record Person(UUID id, String name) {

        static Person from(User user) {
            return new Person(user.getId(), user.getName());
        }
    }

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getKey(),
                task.getProject().getId(),
                task.getSprints().stream()
                        .map(sprint -> new SprintTag(sprint.getId(), sprint.getName()))
                        .sorted(Comparator.comparing(SprintTag::name))
                        .toList(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getExpectedDate(),
                task.getCompletedAt(),
                Person.from(task.getCreatedBy()),
                task.getAssignees().stream()
                        .map(Person::from)
                        .sorted(Comparator.comparing(Person::name))
                        .toList(),
                task.getCreatedAt());
    }
}
