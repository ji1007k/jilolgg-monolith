package com.test.basic.lol;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class RiotApiClient {

    @Value("${lol.riot.api.key}")
    private String riotApiKey;

    private final WebClient webClient = WebClient.create();

    public RiotSummonerResponse fetchSummonerByName(String name) {
        return webClient.get()
                .uri("https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/{name}", name)
                .header("X-Riot-Token", riotApiKey)
                .retrieve()
                .bodyToMono(RiotSummonerResponse.class)
                .block();
    }
}
