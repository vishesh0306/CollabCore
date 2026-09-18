package com.collabflow.task;

import java.util.UUID;

import com.collabflow.shared.CurrentUser;
import com.collabflow.task.dto.CreateTaskRequest;
import com.collabflow.task.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tasks are created inside a project and then used by their key, e.g. /tasks/PAY-12. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
                                   @Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(CurrentUser.id(jwt), projectId, request);
    }

    @GetMapping("/tasks/{key}")
    public TaskResponse getTask(@AuthenticationPrincipal Jwt jwt, @PathVariable String key) {
        return taskService.getTask(CurrentUser.id(jwt), key);
    }
}
