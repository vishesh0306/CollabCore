package com.collabflow.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.collabflow.identity.User;
import com.collabflow.project.Project;
import com.collabflow.sprint.Sprint;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A piece of work in a project, identified by a key like "PAY-12".
 *
 * <p>{@code @SQLRestriction} adds "deleted_at IS NULL" to every query Hibernate runs for
 * tasks, so deleted tasks disappear everywhere without each query having to remember it.
 */
@Entity
@Table(name = "tasks")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Copied from the project, so a team's tasks can be listed without a join. */
    private UUID teamId;

    /** The sprint this task is in, or null when it is in its project's backlog. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    private int number;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private LocalDate expectedDate;

    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // A plain link table with no extra columns, so @ManyToMany is enough. A Set (not a List)
    // because each person is assigned at most once, and Hibernate handles Sets more efficiently.
    @ManyToMany
    @JoinTable(name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> assignees = new HashSet<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant deletedAt;

    /** The expected date the assignees were already reminded about, so it isn't sent twice. */
    private LocalDate overdueRemindedFor;

    public Task(Project project, int number, String title, String description, LocalDate expectedDate,
                User createdBy, Set<User> assignees) {
        this.project = project;
        this.teamId = project.getTeam().getId();
        this.number = number;
        this.title = title;
        this.description = description;
        this.expectedDate = expectedDate;
        this.createdBy = createdBy;
        this.assignees = new HashSet<>(assignees);
        this.status = TaskStatus.TO_DO;
    }

    /** "PAY-12": the project code and this task's number. */
    public String getKey() {
        return project.getCode() + "-" + number;
    }

    /** Did this user create the task, or is it assigned to them? Members may only change such tasks. */
    public boolean belongsTo(UUID userId) {
        return createdBy.getId().equals(userId)
                || assignees.stream().anyMatch(user -> user.getId().equals(userId));
    }

    /** Everyone who cares about this task: whoever created it, and whoever it is assigned to. */
    public Set<UUID> participantIds() {
        Set<UUID> ids = new HashSet<>();
        ids.add(createdBy.getId());
        assignees.forEach(user -> ids.add(user.getId()));
        return ids;
    }

    public Set<UUID> assigneeIds() {
        Set<UUID> ids = new HashSet<>();
        assignees.forEach(user -> ids.add(user.getId()));
        return ids;
    }

    public void updateDetails(String title, String description, LocalDate expectedDate) {
        this.title = title;
        this.description = description;
        this.expectedDate = expectedDate;
    }

    public void changeStatus(TaskStatus newStatus) {
        if (newStatus == TaskStatus.DONE && status != TaskStatus.DONE) {
            completedAt = Instant.now();
        } else if (newStatus != TaskStatus.DONE) {
            completedAt = null;
        }
        status = newStatus;
    }

    public void replaceAssignees(Set<User> newAssignees) {
        assignees.clear();
        assignees.addAll(newAssignees);
    }

    /** Puts the task into a sprint, or back into the backlog with null. */
    public void moveToSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    /** Remembers that the assignees have been told this expected date passed. */
    public void markOverdueReminded() {
        overdueRemindedFor = expectedDate;
    }

    public void delete() {
        deletedAt = Instant.now();
    }
}
