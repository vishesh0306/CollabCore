package com.collabflow.task;

import java.util.UUID;

import com.collabflow.shared.CurrentUser;
import com.collabflow.task.dto.ChangeStatusRequest;
import com.collabflow.task.dto.CreateTaskRequest;
import com.collabflow.task.dto.ReplaceAssigneesRequest;
import com.collabflow.task.dto.TaskResponse;
import com.collabflow.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/tasks/{key}")
    public TaskResponse updateDetails(@AuthenticationPrincipal Jwt jwt, @PathVariable String key,
                                      @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.updateDetails(CurrentUser.id(jwt), key, request);
    }

    @PutMapping("/tasks/{key}/status")
    public TaskResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable String key,
                                     @Valid @RequestBody ChangeStatusRequest request) {
        return taskService.changeStatus(CurrentUser.id(jwt), key, request);
    }

    @PutMapping("/tasks/{key}/assignees")
    public TaskResponse replaceAssignees(@AuthenticationPrincipal Jwt jwt, @PathVariable String key,
                                         @Valid @RequestBody ReplaceAssigneesRequest request) {
        return taskService.replaceAssignees(CurrentUser.id(jwt), key, request);
    }

    @DeleteMapping("/tasks/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@AuthenticationPrincipal Jwt jwt, @PathVariable String key) {
        taskService.deleteTask(CurrentUser.id(jwt), key);
    }
}
