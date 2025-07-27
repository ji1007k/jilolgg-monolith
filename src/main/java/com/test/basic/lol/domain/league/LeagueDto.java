package com.test.basic.lol.domain.league;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeagueDto {

    @JsonProperty("id") // JSON의 id -> leagueId로 매핑
    private String leagueId;

    private String slug;
    private String name;
    private String region;
    private String image;
    private int priority;

    @JsonProperty("displayPriority")
    private DisplayPriority displayPriority;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DisplayPriority {
        private int position;
        private String status;
    }
}
