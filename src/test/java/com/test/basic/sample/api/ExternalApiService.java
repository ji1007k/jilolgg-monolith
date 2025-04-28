package com.test.basic.sample.api;

import com.test.basic.lol.api.LolEsportsApiConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ExternalApiService {

    private final WebClient webClient;

    public ExternalApiService(WebClient.Builder webClientBuilder, LolEsportsApiConfig config) {
        // WebClient가 받아들이는 응답 크기 제한
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024)) // 최대 2MB로 설정 (필요하면 더)
                .build();

        this.webClient = webClientBuilder.baseUrl(config.getUrl())
                .exchangeStrategies(strategies)
                .build();  // JSONPlaceholder API 사용
    }

    // 간단한 GET 요청을 보내고 응답을 반환하는 메서드
    public Mono<String> fetchData() {
        return webClient.get()
                .uri("/todos/1")  // 간단한 TODO API 호출
                .retrieve()
                .bodyToMono(String.class);  // 응답을 String으로 받음
    }
}
