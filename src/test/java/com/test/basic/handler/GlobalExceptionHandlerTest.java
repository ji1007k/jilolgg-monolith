package com.test.basic.handler;

import com.test.basic.users.UserEntity;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @PostMapping("/bad-request")
        public ResponseEntity<String> badRequest(@Valid @RequestBody UserEntity user) {
            return ResponseEntity.badRequest().body("Invalid request");
        }

        @GetMapping("/unauthorized")
        public void unauthorized() {
            throw new AuthenticationException("Unauthorized access") {};
        }

        @GetMapping("/forbidden")
        public void forbidden() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/not-found")
        public void notFound() {
            throw new EmptyResultDataAccessException(1);
        }

        @GetMapping("/conflict")
        public void conflict() {
            throw new DataIntegrityViolationException("Data integrity violation");
        }

        @GetMapping("/server-error")
        public void serverError() {
            throw new RuntimeException("Unexpected error");
        }
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testBadRequest() throws Exception {
        String invalidJson = "{}"; // name 필드 없음 → 검증 실패 발생

        mockMvc.perform(post("/test/bad-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid request")));
    }

    @Test
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Unauthorized")));
    }

    @Test
    void testForbidden() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Forbidden")));
    }

    @Test
    void testNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Resource not found")));
    }

    @Test
    void testConflict() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Cannot process request due to integrity violation")));
    }

    @Test
    void testServerError() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Internal server error")));
    }
}
