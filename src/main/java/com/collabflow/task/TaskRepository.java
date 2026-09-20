package com.collabflow.task;

import java.time.LocalDate;
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

    /** The tasks tagged into a sprint, grouped by project (sorted by project code, then number). */
    @Query("select t from Task t join fetch t.project p join t.sprints s "
            + "where s.id = :sprintId order by p.code, t.number")
    List<Task> findInSprint(@Param("sprintId") UUID sprintId);

    /** The unfinished tasks of a sprint, the ones a carry-over takes into the next sprint. */
    @Query("select t from Task t join fetch t.project join t.sprints s "
            + "where s.id = :sprintId and t.status <> com.collabflow.task.TaskStatus.DONE")
    List<Task> findUnfinishedInSprint(@Param("sprintId") UUID sprintId);

    /**
     * The next tasks that are past their expected date, aren't done, and haven't been
     * reminded about for that date yet.
     *
     * <p>FOR UPDATE locks the rows this run is about to mark, and SKIP LOCKED makes a second
     * copy of the app pick different rows instead of waiting, so the same reminder is never
     * sent twice. Native SQL, so "deleted_at IS NULL" has to be written out here.
     */
    @Query(value = """
            SELECT * FROM tasks
            WHERE deleted_at IS NULL
              AND expected_date < :today
              AND status <> 'DONE'
              AND (overdue_reminded_for IS NULL OR overdue_reminded_for <> expected_date)
            ORDER BY expected_date
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Task> claimOverdueTasks(@Param("today") LocalDate today, @Param("limit") int limit);

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
