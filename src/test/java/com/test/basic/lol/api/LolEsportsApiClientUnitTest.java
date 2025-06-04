package com.test.basic.lol.api;

import com.test.basic.lol.api.esports.LolEsportsApiClient;
import com.test.basic.lol.api.esports.LolEsportsApiConfig;
import com.test.basic.lol.domain.team.TeamService;
import com.test.basic.lol.domain.team.TeamSyncDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

// JUnit5 + Mockito 연동
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
    private TeamService teamService;  // 응답 Spec Mock 객체

    @Mock
    private LolEsportsApiConfig lolEsportsApiConfig;

    private LolEsportsApiClient lolEsportsApiClient;

    private static String MOCK_JSON_RESPONSE;
    private static String MOCK_JSON_RESPONSE2;

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

        MOCK_JSON_RESPONSE2 = "{\n" +
                "  \"data\": {\n" +
                "    \"teams\": [\n" +
                "      {\n" +
                "        \"id\": \"98767991853197861\",\n" +
                "        \"slug\": \"t1\",\n" +
                "        \"name\": \"T1\",\n" +
                "        \"code\": \"T1\",\n" +
                "        \"image\": \"http://static.lolesports.com/teams/1726801573959_539px-T1_2019_full_allmode.png\",\n" +
                "        \"alternativeImage\": \"http://static.lolesports.com/teams/1726801573959_539px-T1_2019_full_allmode.png\",\n" +
                "        \"backgroundImage\": \"http://static.lolesports.com/teams/1596305556675_T1T1.png\",\n" +
                "        \"status\": \"active\",\n" +
                "        \"homeLeague\": {\n" +
                "          \"name\": \"LCK\",\n" +
                "          \"region\": \"한국\"\n" +
                "        },\n" +
                "        \"players\": [\n" +
                "          {\n" +
                "            \"id\": \"102186485482484390\",\n" +
                "            \"summonerName\": \"Doran\",\n" +
                "            \"firstName\": \"Hyeonjun\",\n" +
                "            \"lastName\": \"Choi\",\n" +
                "            \"image\": \"http://static.lolesports.com/players/1739362891866_image6-2025-02-12T132101.302.png\",\n" +
                "            \"role\": \"top\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"105320682452092471\",\n" +
                "            \"summonerName\": \"Oner\",\n" +
                "            \"firstName\": \"HYUNJUN\",\n" +
                "            \"lastName\": \"MUN\",\n" +
                "            \"image\": \"http://static.lolesports.com/players/1739362366214_image6-2025-02-12T131208.507.png\",\n" +
                "            \"role\": \"jungle\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"98767991747728851\",\n" +
                "            \"summonerName\": \"Faker\",\n" +
                "            \"firstName\": \"Sanghyeok\",\n" +
                "            \"lastName\": \"Lee\",\n" +
                "            \"image\": \"http://static.lolesports.com/players/1739362804068_image6-2025-02-12T131922.060.png\",\n" +
                "            \"role\": \"mid\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"108205129774185945\",\n" +
                "            \"summonerName\": \"Smash\",\n" +
                "            \"firstName\": \"Gunmjae\",\n" +
                "            \"lastName\": \"Sin\",\n" +
                "            \"image\": \"http://static.lolesports.com/players/1739361402664_image6-2025-02-12T125536.193.png\",\n" +
                "            \"role\": \"bottom\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\": \"103495716561790834\",\n" +
                "            \"summonerName\": \"Keria\",\n" +
                "            \"firstName\": \"Minseok\",\n" +
                "            \"lastName\": \"Ryu\",\n" +
                "            \"image\": \"http://static.lolesports.com/players/1739362613410_image6-2025-02-12T131347.293.png\",\n" +
                "            \"role\": \"support\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n";
    }

    @BeforeEach
    public void setup() {
        // WebClient.Builder의 메서드 체이닝을 모킹
        when(lolEsportsApiConfig.getUrl()).thenReturn("testUrl");
        when(lolEsportsApiConfig.getKey()).thenReturn("testKey");
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.exchangeStrategies(any(ExchangeStrategies.class))).thenReturn(webClientBuilder);  // exchangeStrategies를 Mock
        when(webClientBuilder.build()).thenReturn(webClient);  // build() 메서드 호출 시, Mock된 WebClient 반환

        // Mocking WebClient Builder 및 WebClient 설정
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        // uri() 메서드의 Function<UriBuilder, URI> 람다를 **Answer**로 처리 (Argument Capturing)
        when(requestHeadersUriSpec.uri(Mockito.any(Function.class))).thenAnswer(invocation -> {
            Function<UriBuilder, URI> uriFunction = invocation.getArgument(0);

            // UriBuilder를 mock으로 생성
            UriBuilder uriBuilder = mock(UriBuilder.class);

            // [핵심] path, queryParam 호출하면 자기 자신 반환하게 설정
            when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
            when(uriBuilder.queryParam(anyString(), anyString())).thenReturn(uriBuilder);
//            when(uriBuilder.queryParam(eq("hl"), anyString())).thenReturn(uriBuilder);
//            when(uriBuilder.queryParam(eq("id"), anyString())).thenReturn(uriBuilder);

            // [핵심] build() 호출하면 정상 URI 반환
            String url = lolEsportsApiConfig.getUrl();
            when(uriBuilder.build()).thenReturn(URI.create(url));

            // 이제 람다 실행
            URI uri = uriFunction.apply(uriBuilder);

            // URI가 null이 아님을 확인
            assertNotNull(uri);
            return requestHeadersUriSpec; // 스텁 결과
        });

//        when(requestHeadersUriSpec.header("x-api-key", lolEsportsApiConfig.getKey())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        lolEsportsApiClient = new LolEsportsApiClient(webClientBuilder, lolEsportsApiConfig);
    }

    /*@Test
    void testFetchScheduleMatches() {
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(MOCK_JSON_RESPONSE));

        // fetchScheduleMatchesJson() 메서드를 호출하여 반환값을 확인
        Mono<String> result = lolEsportsApiClient.fetchScheduleMatchesJson();

        // block()으로 결과 값을 동기적으로 받습니다.
        String jsonResponse = result.block();

        // parseMatchesFromResponse가 JSON String을 받아서 파싱을 진행하도록 설정
        List<MatchDto> matches = lolEsportsApiClient.parseMatchesFromResponse(jsonResponse, "");

        // 결과가 비어 있지 않으며, 예상한 크기 이상인지를 확인
        assertThat(matches).isNotNull();
        assertThat(matches.size()).isGreaterThan(0);
    }*/


    @Test
    void testFetchAllTeams() {
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(MOCK_JSON_RESPONSE2));
        when(teamService.parseTeamsFromResponse(anyString())).thenReturn(List.of(new TeamSyncDto()));

        // 실행
        Mono<String> result = lolEsportsApiClient.fetchAllTeams();
        List<TeamSyncDto> teams = teamService.parseTeamsFromResponse(result.block());

        // 검증
        assertThat(teams).isNotNull();
        assertThat(teams.size()).isGreaterThan(0);

        // verify() 캡쳐 검증은 여기
        ArgumentCaptor<Function<UriBuilder, URI>> captor = ArgumentCaptor.forClass(Function.class);
        verify(requestHeadersUriSpec).uri(captor.capture());

        // 캡쳐된 function을 다시 실행해서 URI를 검증
        UriBuilder fakeUriBuilder = mock(UriBuilder.class);
        when(fakeUriBuilder.path(anyString())).thenReturn(fakeUriBuilder);
        when(fakeUriBuilder.queryParam(anyString(), any(Object.class))).thenReturn(fakeUriBuilder);
        String testUrl = lolEsportsApiConfig.getUrl();
        when(fakeUriBuilder.build()).thenReturn(URI.create(testUrl));

        URI capturedUri = captor.getValue().apply(fakeUriBuilder);
        assertEquals(lolEsportsApiConfig.getUrl(), capturedUri.toString());
    }

    @Test
    void testFetchTeamBySlug() {
        TeamSyncDto resultDto = new TeamSyncDto();
        resultDto.setSlug("t1");

        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(MOCK_JSON_RESPONSE2));
        when(teamService.parseTeamsFromResponse(anyString())).thenReturn(List.of(resultDto));

        Mono<String> result = lolEsportsApiClient.fetchTeamBySlug("t1");
        List<TeamSyncDto> teams = teamService.parseTeamsFromResponse(result.block());

        assertThat(teams).isNotNull();
        assertThat(teams.size()).isGreaterThan(0);
        assertThat(teams.get(0).getSlug()).isEqualTo("t1");

        // 없는 결과
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(""));
        when(teamService.parseTeamsFromResponse(anyString())).thenReturn(Collections.emptyList());
        Mono<String> result2 = lolEsportsApiClient.fetchTeamBySlug("DoesNotExist");
        List<TeamSyncDto> teams2 = teamService.parseTeamsFromResponse(result2.block());

        assertThat(teams2).isNotNull();
        assertThat(teams2.size()).isEqualTo(0);
    }

}
