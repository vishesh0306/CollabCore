package com.collabflow.task;

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
