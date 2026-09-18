package com.collabflow.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checks the error format using a small controller that exists only in this test.
 * Spring's scanning skips classes nested in tests, so the controller is imported explicitly.
 * Security filters are switched off because they aren't what's being tested here.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.SampleController.class)
@Import(GlobalExceptionHandlerTest.SampleController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundIsReturnedAsProblemDetail() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Task PAY-99 was not found"));
    }

    @Test
    void invalidFieldsAreListedByName() throws Exception {
        mockMvc.perform(post("/test/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void unexpectedErrorsDoNotLeakDetails() throws Exception {
        mockMvc.perform(get("/test/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Something went wrong. Please try again."));
    }

    @RestController
    static class SampleController {

        @GetMapping("/test/missing")
        void missing() {
            throw new NotFoundException("Task PAY-99 was not found");
        }

        @PostMapping("/test/users")
        void createUser(@Valid @RequestBody SampleUser user) {
        }

        @GetMapping("/test/crash")
        void crash() {
            throw new IllegalStateException("internal detail the client must not see");
        }
    }

    record SampleUser(@NotBlank String name, @Email String email) {
    }
}
