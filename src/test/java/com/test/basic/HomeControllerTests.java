package com.test.basic;
//package example.web;

/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.nimbusds.jose.util.Base64;
import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.sample.TokenController;
import com.test.basic.auth.security.config.SecurityConfig;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link HomeController}
 *
 * @author Josh Cummings
 */
@WebMvcTest({ HomeController.class, TokenController.class })
@Import(SecurityConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HomeControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

//    UserDetails mockUser;

    @BeforeAll
    void setup() {
        // ======================
        // 가짜 UserDetails 설정
       /* mockUser = new CustomUserDetails(
                1L, // 혹은 UUID.randomUUID()
                "admin",           // email
                "$2b$12$JgK.Du5J.DbMQ6zQ1Tx58OoKCEGr3NUG.p45zDQb0qALy9T5MczJy", // password
                "admin",           // username
                List.of(new SimpleGrantedAuthority("SCOPE_ADMIN"))
        );*/
    }


    @Test
    void testTokenGeneration_withCredentials200() throws Exception {
        String userInfo = "admin:admin";
        Base64 base64Encoded = Base64.encode(userInfo.getBytes(StandardCharsets.UTF_8));

        // @formatter:off
        MvcResult result2 = this.mvc.perform(get("/token/generate")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Encoded)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String token = result2.getResponse().getContentAsString();

        assertNotNull(token);
    }

    @Test
    void testTokenGeneration_badCredentials401() throws Exception {
        // @formatter:off
        this.mvc.perform(get("/token/generate"))
                .andExpect(status().isUnauthorized());
        // @formatter:on
    }

    @Test
    void testIndexPage_moveSuccessfully() throws Exception {
        // @formatter:off
        this.mvc.perform(get("/"))
                .andExpect(status().isOk());
        // @formatter:on
    }

}