package com.collabflow.comment;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByTaskId(UUID taskId, Pageable pageable);

    /** The comment, but only if its task hasn't been deleted: a deleted task takes its comments with it. */
    @Query("select c from Comment c join fetch c.task t where c.id = :id and t.deletedAt is null")
    Optional<Comment> findWithLiveTask(@Param("id") UUID id);
}
