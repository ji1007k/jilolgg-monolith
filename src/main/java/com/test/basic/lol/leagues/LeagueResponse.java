package com.test.basic.lol.leagues;

import lombok.Data;

import java.util.List;

@Data
public class LeagueResponse {
    private LeagueData data;

    @Data
    public static class LeagueData {
        private List<LeagueDto> leagues;
    }
}

