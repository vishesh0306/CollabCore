package com.collabflow.comment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.collabflow.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CommentControllerTest extends ApiTest {

    private TestUser manager;
    private TestUser member;
    private TestUser otherMember;
    private TestProject project;
    private String taskKey;

    /** A team (a manager and two members), a project, and one task to comment on. */
    @BeforeEach
    void setUp() throws Exception {
        manager = newUser();
        member = newUser();
        otherMember = newUser();
        UUID teamId = createTeam(manager);
        addToTeam(teamId, member, "MEMBER");
        addToTeam(teamId, otherMember, "MEMBER");
        project = createProject(manager, teamId);
        String body = mockMvc.perform(post("/api/v1/projects/{id}/tasks", project.id())
                        .header("Authorization", member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Refund API\"}"))
                .andReturn().getResponse().getContentAsString();
        taskKey = JsonPath.read(body, "$.key");
    }

    @Test
    void anyoneInTheTeamCanCommentAndReadComments() throws Exception {
        addComment(otherMember, "Is the API spec ready?")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.id").value(otherMember.id().toString()))
                .andExpect(jsonPath("$.body").value("Is the API spec ready?"))
                .andExpect(jsonPath("$.editedAt").doesNotExist());
        addComment(member, "Yes, linked in the description.");

        listComments(manager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].body").value("Is the API spec ready?")) // oldest first
                .andExpect(jsonPath("$.items[1].body").value("Yes, linked in the description."));
    }

    @Test
    void outsidersCanNeitherCommentNorRead() throws Exception {
        TestUser outsider = newUser();

        addComment(outsider, "Hello?").andExpect(status().isNotFound());
        listComments(outsider).andExpect(status().isNotFound());
    }

    @Test
    void emptyCommentsAreRejected() throws Exception {
        addComment(member, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").exists());
    }

    @Test
    void onlyTheAuthorCanEditAComment() throws Exception {
        String commentId = addCommentAndGetId(member, "First draft");

        editComment(member, commentId, "Second draft")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Second draft"))
                .andExpect(jsonPath("$.editedAt").isNotEmpty());
        editComment(manager, commentId, "Words in your mouth")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the author can edit a comment"));
    }

    @Test
    void theAuthorOrAManagerCanDeleteAComment() throws Exception {
        String byMember = addCommentAndGetId(member, "Mine");
        String byOther = addCommentAndGetId(otherMember, "Theirs");

        deleteComment(member, byOther)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the author or a manager can delete a comment"));
        deleteComment(member, byMember).andExpect(status().isNoContent());
        deleteComment(manager, byOther).andExpect(status().isNoContent());

        listComments(member).andExpect(jsonPath("$.totalItems").value(0));
        editComment(member, byMember, "Too late").andExpect(status().isNotFound());
    }

    @Test
    void commentsOfACompletedProjectCannotChange() throws Exception {
        String commentId = addCommentAndGetId(member, "Before completion");
        mockMvc.perform(post("/api/v1/projects/{id}/complete", project.id()).header("Authorization", manager.token()));

        addComment(member, "After completion").andExpect(status().isConflict());
        editComment(member, commentId, "Edited later").andExpect(status().isConflict());
        listComments(member).andExpect(jsonPath("$.totalItems").value(1)); // still readable
    }

    @Test
    void aDeletedTaskTakesItsCommentsWithIt() throws Exception {
        String commentId = addCommentAndGetId(member, "About this task");
        mockMvc.perform(delete("/api/v1/tasks/{key}", taskKey).header("Authorization", manager.token()));

        listComments(member).andExpect(status().isNotFound());
        editComment(member, commentId, "Still here?").andExpect(status().isNotFound());
    }

    // --- helpers ---

    private ResultActions addComment(TestUser user, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/tasks/{key}/comments", taskKey)
                .header("Authorization", user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\"}"));
    }

    private String addCommentAndGetId(TestUser user, String body) throws Exception {
        return JsonPath.read(addComment(user, body).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions listComments(TestUser user) throws Exception {
        return mockMvc.perform(get("/api/v1/tasks/{key}/comments", taskKey).header("Authorization", user.token()));
    }

    private ResultActions editComment(TestUser user, String commentId, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/comments/{id}", commentId)
                .header("Authorization", user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\"}"));
    }

    private ResultActions deleteComment(TestUser user, String commentId) throws Exception {
        return mockMvc.perform(delete("/api/v1/comments/{id}", commentId).header("Authorization", user.token()));
    }
}
