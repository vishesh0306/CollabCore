package com.collabflow.task.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.collabflow.project.Project;
import com.collabflow.shared.CompanyTime;
import com.collabflow.sprint.Sprint;
import com.collabflow.sprint.SprintStatus;
import com.collabflow.task.Task;
import com.collabflow.task.TaskStatus;

/**
 * The sprint page: what the sprint is for, how long is left, and every task tagged into it,
 * grouped by project with progress. A finished sprint keeps its tasks, so this stays useful
 * afterwards; it shows them as they are now, not as they were.
 */
public record SprintPageResponse(UUID sprintId,
                                 String name,
                                 String target,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 SprintStatus status,
                                 Long daysLeft,
                                 int done,
                                 int total,
                                 List<ProjectTasks> projects) {

    public record ProjectTasks(UUID projectId, String code, String name, int done, int total,
                               List<TaskResponse> tasks) {
    }

    /** Expects the tasks sorted by project, so each project's tasks come together. */
    public static SprintPageResponse from(Sprint sprint, List<Task> tasks) {
        Map<Project, List<Task>> byProject = new LinkedHashMap<>();
        for (Task task : tasks) {
            byProject.computeIfAbsent(task.getProject(), project -> new ArrayList<>()).add(task);
        }
        List<ProjectTasks> groups = byProject.entrySet().stream()
                .map(entry -> new ProjectTasks(
                        entry.getKey().getId(),
                        entry.getKey().getCode(),
                        entry.getKey().getName(),
                        countDone(entry.getValue()),
                        entry.getValue().size(),
                        entry.getValue().stream().map(TaskResponse::from).toList()))
                .toList();
        return new SprintPageResponse(sprint.getId(), sprint.getName(), sprint.getTarget(),
                sprint.getStartDate(), sprint.getEndDate(), sprint.getStatus(), daysLeft(sprint),
                countDone(tasks), tasks.size(), groups);
    }

    /**
     * Days until the end date, negative once it has passed. Empty for a finished sprint, where
     * the question no longer means anything.
     */
    private static Long daysLeft(Sprint sprint) {
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            return null;
        }
        return ChronoUnit.DAYS.between(CompanyTime.today(), sprint.getEndDate());
    }

    private static int countDone(List<Task> tasks) {
        return (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
    }
}
