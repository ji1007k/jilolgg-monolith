package com.test.basic.lol.api;

import com.test.basic.lol.matches.MatchDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // Mockito 확장 기능을 활성화
public class LolEsportsApiClientUnitTest {

    @Mock
    private WebClient.Builder webClientBuilder;  // WebClient.Builder Mock 객체

    @Mock
    private WebClient webClient;  // WebClient Mock 객체

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;  // URI 요청 Mock 객체

    @Mock
    private WebClient.ResponseSpec responseSpec;  // 응답 Spec Mock 객체

    @Mock
    private LolEsportsApiConfig lolEsportsApiConfig;

    private LolEsportsApiClient lolEsportsApiClient;

    private static String MOCK_JSON_RESPONSE;

    @BeforeAll
    static void setupMockJsonResponse() {
        MOCK_JSON_RESPONSE  = "{\n" +
                "  \"data\": {\n" +
                "    \"schedule\": {\n" +
                "      \"events\": [\n" +
                "        {\n" +
                "          \"startTime\": \"2025-04-28T12:00:00Z\",\n" +
                "          \"state\": \"completed\",\n" +
                "          \"match\": {\n" +
                "            \"teams\": [\n" +
                "              { \"code\": \"team1\", \"name\": \"Team 1\", \"result\": {\"outcome\": \"win\"} },\n" +
                "              { \"code\": \"team2\", \"name\": \"Team 2\", \"result\": {\"outcome\": \"lose\"} }\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    @BeforeEach
    public void setup() {

        // WebClient.Builder의 메서드 체이닝을 모킹
        when(lolEsportsApiConfig.getUrl()).thenReturn("testUrl");
        when(lolEsportsApiConfig.getKey()).thenReturn("testKey");
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.exchangeStrategies(any(ExchangeStrategies.class))).thenReturn(webClientBuilder);  // exchangeStrategies를 Mock
        when(webClientBuilder.build()).thenReturn(webClient);  // build() 메서드 호출 시, Mock된 WebClient 반환

        // Mocking WebClient Builder 및 WebClient 설정
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        // uri() 메서드의 Function<UriBuilder, URI> 람다를 **Answer**로 처리
        when(requestHeadersUriSpec.uri(Mockito.any(Function.class))).thenAnswer(invocation -> {
            Function<UriBuilder, URI> uriFunction = invocation.getArgument(0);

            // UriBuilder를 mock으로 생성
            UriBuilder uriBuilder = mock(UriBuilder.class);

            // [핵심] path, queryParam 호출하면 자기 자신 반환하게 설정
            when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
            when(uriBuilder.queryParam(anyString(), anyString())).thenReturn(uriBuilder);

            // [핵심] build() 호출하면 정상 URI 반환
            when(uriBuilder.build()).thenReturn(URI.create("http://localhost/test"));

            // 이제 람다 실행
            URI uri = uriFunction.apply(uriBuilder);

            // URI가 null이 아님을 확인
            assertNotNull(uri);
            return requestHeadersUriSpec; // 스텁 결과
        });

        when(requestHeadersUriSpec.header("x-api-key", lolEsportsApiConfig.getKey())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(MOCK_JSON_RESPONSE));

        lolEsportsApiClient = new LolEsportsApiClient(webClientBuilder, lolEsportsApiConfig);
    }

    @Test
    void testFetchScheduleMatches() {
        // fetchScheduleMatches() 메서드를 호출하여 반환값을 확인
        Mono<String> result = lolEsportsApiClient.fetchScheduleMatches();

        // block()으로 결과 값을 동기적으로 받습니다.
        String jsonResponse = result.block();

        // parseMatchesFromResponse가 JSON String을 받아서 파싱을 진행하도록 설정
        List<MatchDto> matches = lolEsportsApiClient.parseMatchesFromResponse(jsonResponse);

        // 결과가 비어 있지 않으며, 예상한 크기 이상인지를 확인
        assertThat(matches).isNotNull();
        assertThat(matches.size()).isGreaterThan(0);
    }
}
