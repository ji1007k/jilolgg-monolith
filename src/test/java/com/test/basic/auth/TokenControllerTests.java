package com.test.basic.auth;
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

import com.test.basic.HomeController;
import com.test.basic.auth.sample.TokenController;
import com.test.basic.auth.security.config.SecurityConfig;
import com.test.basic.auth.security.user.CustomUserDetailsService;
import com.test.basic.common.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link HomeController}
 *
 * @author Josh Cummings
 */
@WebMvcTest(TokenController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, AuthTestSupport.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuthTestSupport authTestSupport;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;


    @Test
    void testTokenGeneration_withCredentials200() throws Exception {
        UserDetails mockUser = authTestSupport.createTestAdminUser();
        when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(mockUser);

        String token = authTestSupport.createJwtTokenStr("admin", "admin");
        assertNotNull(token);
    }

    @Test
    void testTokenGeneration_badCredentials401() throws Exception {
        // @formatter:off
        this.mvc.perform(get("/token/generate"))
                .andExpect(status().isUnauthorized());
        // @formatter:on
    }
}