package com.test.basic.lol.matches;


import lombok.Data;

import java.util.List;

@Data
public class MatchScheduleResponse {
    private MatchScheduleData data;

    @Data
    public static class MatchScheduleData {
        private ScheduleDto schedule;
    }

    @Data
    public static class ScheduleDto {
        private PagesDto pages;
        private List<EventDto> events;
    }

    @Data
    public static class PagesDto {
        private String older;
        private String newer;
    }

    @Data
    public static class EventDto {
        private String startTime;
        private String state;
        private String type;
        private String blockName;
        private LeagueDto league;
        private MatchDto match;
    }

    @Data
    public static class LeagueDto {
        private String name;
        private String slug;
    }

    @Data
    public static class MatchDto {
        private String id;
        private List<String> flags;
        private List<TeamDto> teams;
        private StrategyDto strategy;
    }

    @Data
    public static class TeamDto {
        private String name;
        private String code;
        private String image;
        private ResultDto result;
        private RecordDto record;
    }

    @Data
    public static class ResultDto {
        private String outcome;
        private int gameWins;
    }

    @Data
    public static class RecordDto {
        private int wins;
        private int losses;
    }

    @Data
    public static class StrategyDto {
        private String type;
        private int count;
    }
}
