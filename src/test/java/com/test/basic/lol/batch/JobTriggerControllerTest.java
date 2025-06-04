package com.test.basic.lol.batch;

import com.test.basic.auth.jwt.JwtTokenProvider;
import com.test.basic.auth.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest를 쓰면 기본적으로 Spring Security 필터가 작동, 인증 필요
@WebMvcTest(controllers = JobTriggerController.class)
@ExtendWith(MockitoExtension.class)
@Import(SecurityConfig.class)
public class JobTriggerControllerTest {

    @Autowired
    private MockMvc mockMvc;

//    @Autowired
//    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JobLauncher jobLauncher;

    @MockBean
    private Job job;

    @Test
    void testRunSampleJob_should_complete_successfully() throws Exception {
        MvcResult result = mockMvc.perform(get("/lol/batch/run-sample"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo("Job 실행 완료!");
    }
}
