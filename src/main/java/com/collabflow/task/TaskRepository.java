package com.collabflow.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** JpaSpecificationExecutor adds findAll(Specification, Pageable) for the filtered task list. */
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    /** Finds "PAY-12" by project code and number. */
    @Query("select t from Task t join fetch t.project p where p.code = :code and t.number = :number")
    Optional<Task> findByKey(@Param("code") String code, @Param("number") int number);

    /** A sprint's tasks, grouped by project (sorted by project code, then task number). */
    @Query("select t from Task t join fetch t.project p where t.sprint.id = :sprintId order by p.code, t.number")
    List<Task> findInSprint(@Param("sprintId") UUID sprintId);

    /**
     * Sends a sprint's unfinished tasks back to the backlog, in one SQL statement.
     * A bulk update like this goes straight to the database: it doesn't update task objects
     * already loaded in memory, and @UpdateTimestamp isn't applied to the rows it changes.
     */
    @Modifying
    @Query("update Task t set t.sprint = null "
            + "where t.sprint.id = :sprintId and t.status <> com.collabflow.task.TaskStatus.DONE")
    int moveUnfinishedTasksToBacklog(@Param("sprintId") UUID sprintId);

    /**
     * Unassigns a person from every task of one team, in a single SQL statement.
     * Native SQL because it works on the link table directly, without loading the tasks.
     */
    @Modifying
    @Query(value = """
            DELETE FROM task_assignees
            WHERE user_id = :userId
              AND task_id IN (SELECT id FROM tasks WHERE team_id = :teamId)
            """, nativeQuery = true)
    int removeAssigneeFromTeamTasks(@Param("teamId") UUID teamId, @Param("userId") UUID userId);
}
