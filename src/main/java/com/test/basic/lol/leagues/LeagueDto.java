package com.test.basic.lol.leagues;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
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

    @Data
    public static class DisplayPriority {
        private int position;
        private String status;
    }
}
