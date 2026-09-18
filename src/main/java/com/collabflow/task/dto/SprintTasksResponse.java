package com.collabflow.task.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.collabflow.project.Project;
import com.collabflow.task.Task;
import com.collabflow.task.TaskStatus;

/** A sprint's tasks grouped by project, with how many are done, per project and overall. */
public record SprintTasksResponse(UUID sprintId, int done, int total, List<ProjectTasks> projects) {

    public record ProjectTasks(UUID projectId, String code, String name, int done, int total,
                               List<TaskResponse> tasks) {
    }

    /** Expects the tasks sorted by project, so each project's tasks come together. */
    public static SprintTasksResponse from(UUID sprintId, List<Task> tasks) {
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
        return new SprintTasksResponse(sprintId, countDone(tasks), tasks.size(), groups);
    }

    private static int countDone(List<Task> tasks) {
        return (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
    }
}
