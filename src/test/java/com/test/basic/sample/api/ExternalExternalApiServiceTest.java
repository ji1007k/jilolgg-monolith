package com.test.basic.sample.api;

import com.test.basic.lol.api.LolEsportsApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * WebClient, RequestHeadersUriSpec, ResponseSpec 전부 모킹
 * 실제 HTTP 안 나감
 */
@ExtendWith(MockitoExtension.class)  // Mockito 확장 기능을 활성화
public class ExternalExternalApiServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;  // WebClient.Builder Mock 객체

    @Mock
    private WebClient webClient;  // WebClient Mock 객체

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;  // URI 요청 Mock 객체

    @Mock
    private WebClient.ResponseSpec responseSpec;  // 응답 Spec Mock 객체

    @Mock
    private LolEsportsApiConfig config;  // 응답 Spec Mock 객체

    private ExternalApiService externalApiService;  // 테스트 대상 서비스 클래스

    @BeforeEach
    public void setup() {
        // WebClient.Builder에서 WebClient를 빌드할 때 Mock WebClient를 반환하도록 설정
        when(config.getUrl()).thenReturn("https://jsonplaceholder.typicode.com");
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.exchangeStrategies(any(ExchangeStrategies.class))).thenReturn(webClientBuilder);  // exchangeStrategies를 Mock
        when(webClientBuilder.build()).thenReturn(webClient);  // build() 메서드 호출 시, Mock된 WebClient 반환

        // WebClient의 동작을 설정
        when(webClient.get()).thenReturn(requestHeadersUriSpec);  // get() 호출 시, requestHeadersUriSpec 반환
        when(requestHeadersUriSpec.uri("/todos/1")).thenReturn(requestHeadersUriSpec);  // URI 설정
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);  // retrieve() 호출 시, responseSpec 반환
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"userId\": 1, \"id\": 1, \"title\": \"delectus aut autem\", \"completed\": false}"));  // 응답 설정

        externalApiService = new ExternalApiService(webClientBuilder, config);  // Mock된 WebClient.Builder를 사용하여 ApiService 인스턴스 생성
    }

    @Test
    void testFetchData() {
        // fetchData 메서드를 호출하여 응답을 받음
        Mono<String> result = externalApiService.fetchData();

        // Mono의 결과를 가져와서 검증
        String response = result.block();  // block()을 사용하여 결과를 동기적으로 가져옴

        // 결과가 null이 아니고, 예상한 JSON 응답이 포함된 문자열인지 검증
        assertThat(response).isNotNull();
        assertThat(response).contains("\"userId\": 1");
        assertThat(response).contains("\"title\": \"delectus aut autem\"");
    }
}
