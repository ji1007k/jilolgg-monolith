package com.test.basic.auth.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)  // Controller만 테스트하기 위해 사용
@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // MockMvc를 사용하여 HTTP 요청을 테스트

    @Autowired
    private ObjectMapper objectMapper;  // ObjectMapper를 사용하여 JSON 변환

    // UserService를 Mock 처리하여 실제 서비스 호출을 Mocking
    // @MockBean을 사용했지만, Spring Boot 3.4부터는 @MockitoBean을 사용
    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 테스트에서 사용할 객체 준비
    }

    @Test
    @DisplayName("유저 생성 - HTTP 응답만 검증")
    void testCreateUser() throws Exception {
        // 테스트용 유저 데이터 (서비스 없이 컨트롤러 단위 테스트)
        User newUser = new User(1L, "password123", "email@example.com", "username", null, null, null);

        // Mocking된 서비스가 반환할 유저 설정
        given(userService.createUser(any(User.class))).willReturn(newUser);

        // 서비스 없이 컨트롤러에서 HTTP 응답만 확인
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())  // HTTP 201 상태 확인
                .andExpect(jsonPath("$.email").value("email@example.com"))  // 이메일 검증
                .andExpect(jsonPath("$.name").value("username"));  // 이름 검증
    }

    @Test
    @DisplayName("유저 목록 조회 - HTTP 응답만 검증")
    void testGetUsers() throws Exception {
        // 테스트용 유저 데이터 (서비스 없이 컨트롤러 단위 테스트)
        User newUser = new User(null, "password123", "email@example.com", "username", null, null, null);

        List<User> users = List.of(newUser);

        // Mocking된 서비스가 반환할 유저 목록 설정
        given(userService.getAllUsers(1, 10, "", "id,asc")).willReturn(users);

        // GET 요청으로 유저 목록 조회
        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())  // HTTP 200 상태 확인
                .andExpect(jsonPath("$").isArray())  // 배열 형태로 응답인지 확인
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(1)));  // 1개 이상의 유저가 있는지 확인
    }

    @Test
    @DisplayName("유저 단건 조회 - HTTP 응답만 검증")
    void testGetUserById() throws Exception {
        // 테스트용 유저 데이터 (서비스 없이 컨트롤러 단위 테스트)
        User newUser = new User(1L, "password123", "email@example.com", "username", null, null, null);

        // Mocking된 서비스가 반환할 유저 설정
        given(userService.getUserById(1L)).willReturn(Optional.of(newUser));

        // 특정 ID로 유저 조회
        mockMvc.perform(get("/api/users/{id}", 1L))  // 예시 ID: 1
                .andExpect(status().isOk())  // HTTP 200 상태 확인
                .andExpect(jsonPath("$.id").value(1L))  // ID 검증
                .andExpect(jsonPath("$.email").value("email@example.com"));  // 이메일 검증
    }

    @Test
    @DisplayName("유저 수정 - HTTP 응답만 검증")
    void testUpdateUser() throws Exception {
        // 수정하려는 기존 유저 데이터 (서비스 없이 컨트롤러 단위 테스트)
        User existingUser = new User(1L, "password123", "username@test.com", "username", null, null, null);
        User updatedUser = new User();
        updatedUser.setId(existingUser.getId());// 수정할 유저의 ID를 설정
        updatedUser.setName("newname");// 수정할 유저의 ID를 설정

        // Mocking된 서비스가 반환할 유저 설정
//        given(userService.updateUser(1L, any(User.class))).willReturn(Optional.of(updatedUser));
        // Mockito는 모든 인자에 ArgumentMatchers를 사용해야 함
        given(userService.updateUser(eq(1L), any(User.class))).willReturn(Optional.of(updatedUser));

        // 특정 ID로 유저 수정
        mockMvc.perform(put("/api/users/{id}", 1L)  // URL 경로에서 id 값을 1L로 전달
                        .contentType(MediaType.APPLICATION_JSON)  // JSON 형식으로 요청
                        .content(objectMapper.writeValueAsString(updatedUser)))  // updatedUser를 JSON으로 변환하여 본문에 담기
//                .andExpect(status().isOk())  // HTTP 200 상태 확인
                .andExpect(jsonPath("$.id").value(1L))  // ID가 1L인지 확인
                .andExpect(jsonPath("$.name").value(updatedUser.getName()));  // 이름이 "newusername"인지 확인
    }

    @Test
    @DisplayName("유저 삭제 - HTTP 응답만 검증")
    void testDeleteUser() throws Exception {
        // UserService의 deleteUser 메서드를 호출할 때, 아무것도 반환하지 않도록 설정
        doNothing().when(userService).deleteUser(eq(1L));

        // DELETE 요청을 보내고 HTTP 상태 코드가 204 (No Content)인지를 검증
        mockMvc.perform(delete("/api/users/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
