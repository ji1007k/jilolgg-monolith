package com.test.basic.lol.api.dto.matches;

import lombok.Data;
import java.util.List;

@Data
public class MatchDetailResponse {
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private EventDto event;
    }

    @Data
    public static class EventDto {
        private String id;
        private String type;
        private TournamentDto tournament;
        private LeagueDto league;
        private MatchDto match;
        private List<StreamDto> streams; // 비어있지만 포함
    }

    @Data
    public static class TournamentDto {
        private String id;
    }

    @Data
    public static class LeagueDto {
        private String id;
        private String slug;
        private String image;
        private String name;
    }

    @Data
    public static class MatchDto {
        private StrategyDto strategy;
        private List<TeamDto> teams;
        private List<GameDto> games;
    }

    @Data
    public static class StrategyDto {
        private int count;
    }

    @Data
    public static class TeamDto {
        private String id;
        private String name;
        private String code;
        private String image;
        private ResultDto result;
    }

    @Data
    public static class ResultDto {
        private int gameWins;
    }

    @Data
    public static class GameDto {
        private int number;
        private String id;
        private String state;
        private List<GameTeamDto> teams;
        private List<VodDto> vods;
    }

    @Data
    public static class GameTeamDto {
        private String id;
        private String side;
    }

    @Data
    public static class VodDto {
        private String id;
        private String parameter;
        private String locale;
        private String provider;
        private int offset;
        private String firstFrameTime;
        private Long startMillis;
        private Long endMillis;
        private MediaLocaleDto mediaLocale;
    }

    @Data
    public static class MediaLocaleDto {
        private String locale;
        private String englishName;
        private String translatedName;
    }

    @Data
    public static class StreamDto {
        // TODO 필요 시 정의
        /*"streams": [
        {
            "parameter": "lck",
                "locale": "en-US",
                "mediaLocale": {
            "locale": "en-US",
                    "englishName": "English (North America)",
                    "translatedName": "English (North America)"
        },
            "provider": "twitch",
                "countries": [],
            "offset": -30000,
                "statsStatus": "enabled"
        },
        {
            "parameter": "aflol",
                "locale": "ko-KR",
                "mediaLocale": {
            "locale": "ko-KR",
                    "englishName": "Korean (Korea)",
                    "translatedName": "한국어"
        },
            "provider": "afreecatv",
                "countries": [],
            "offset": -35000,
                "statsStatus": "enabled"
        },
        {
            "parameter": "otplol_",
                "locale": "fr-FR",
                "mediaLocale": {
            "locale": "fr-FR",
                    "englishName": "French (France)",
                    "translatedName": "Français"
        },
            "provider": "twitch",
                "countries": [
            "FR"
                    ],
            "offset": -30000,
                "statsStatus": "enabled"
        }
            ]*/
    }
}
