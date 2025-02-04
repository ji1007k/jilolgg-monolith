package com.test.basic.auth.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("User API - HTTP 응답 테스트")
public class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;  // ObjectMapper를 주입

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "password", "email", "name", "profileImageUrl", null, null);
    }

    @Test
    void testSignUp() throws Exception {
        String userJson = objectMapper.writeValueAsString(user);

        mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(userJson))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetUsersWithPaginationAndSearch() throws Exception {
        // 예시: /api/users?page=0&size=10&keyword=jikim
        mockMvc.perform(get("/api/users")
                    .param("page", "0")
                    .param("size", "10")
                    .param("keyword", "jikim")
                    .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(0)));
    }

    @Test
    void testGetUserById() throws Exception {
        Long userId = 1L;

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateUser() throws Exception {
        Long userId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/users/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void testDeleteUser() throws Exception {
        Long userId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());
    }

}
