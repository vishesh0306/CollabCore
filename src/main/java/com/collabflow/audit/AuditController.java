package com.collabflow.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.collabflow.audit.dto.AuditEntryResponse;
import com.collabflow.audit.dto.AuditPage;
import com.collabflow.shared.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reading the log. Each list is paged with {@code before} and {@code size}: take
 * {@code nextBefore} from a response and send it back as {@code before} for the next page.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    /** One task's whole history, newest first. */
    @GetMapping("/tasks/{key}/history")
    public List<AuditEntryResponse> taskHistory(@AuthenticationPrincipal Jwt jwt, @PathVariable String key) {
        return auditQueryService.taskHistory(CurrentUser.id(jwt), key);
    }

    /** Everything that happened in a team. Its members and the admin can read it. */
    @GetMapping("/teams/{teamId}/activity")
    public AuditPage teamActivity(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID teamId,
                                  @RequestParam(required = false) Long before,
                                  @RequestParam(required = false) Integer size) {
        return auditQueryService.teamActivity(CurrentUser.id(jwt), teamId, before, size);
    }

    /** A project, its tasks and their comments. */
    @GetMapping("/projects/{projectId}/activity")
    public AuditPage projectActivity(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID projectId,
                                     @RequestParam(required = false) Long before,
                                     @RequestParam(required = false) Integer size) {
        return auditQueryService.projectActivity(CurrentUser.id(jwt), projectId, before, size);
    }

    /** A sprint's timeline: the sprint and everything about the tasks tagged into it. */
    @GetMapping("/sprints/{sprintId}/timeline")
    public AuditPage sprintTimeline(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable UUID sprintId,
                                    @RequestParam(required = false) Long before,
                                    @RequestParam(required = false) Integer size) {
        return auditQueryService.sprintTimeline(CurrentUser.id(jwt), sprintId, before, size);
    }

    /**
     * The whole company's log, for the admin only. Every filter is optional, e.g.
     * {@code /audit?entityType=TASK&action=DELETED&from=2026-10-01T00:00:00Z}.
     */
    @GetMapping("/audit")
    public AuditPage companyLog(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam(required = false) UUID teamId,
                                @RequestParam(required = false) UUID actorId,
                                @RequestParam(required = false) AuditEntityType entityType,
                                @RequestParam(required = false) UUID entityId,
                                @RequestParam(required = false) AuditAction action,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                @RequestParam(required = false) Long before,
                                @RequestParam(required = false) Integer size) {
        return auditQueryService.companyLog(CurrentUser.id(jwt), teamId, actorId, entityType, entityId,
                action, from, to, before, size);
    }
}
