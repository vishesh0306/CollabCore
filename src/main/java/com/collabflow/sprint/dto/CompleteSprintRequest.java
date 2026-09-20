package com.collabflow.sprint.dto;

import java.util.UUID;

/**
 * How to finish a sprint. With a sprint named here, every unfinished task of the sprint being
 * completed is tagged into that one as well, so the work continues there without disappearing
 * from this sprint. Leave it out to only change the status.
 */
public record CompleteSprintRequest(UUID carryOverToSprintId) {
}
