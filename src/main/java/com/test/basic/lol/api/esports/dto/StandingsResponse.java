package com.test.basic.lol.api.esports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StandingsResponse {

    private StandingsData data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StandingsData {
        private String tournamentId;      // tournamentId
        List<StandingsDto> standings;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StandingsDto {
        public List<StageDto> stages;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StageDto {
        public String id;
        public String name;
        public String type;
        public String slug;
        public List<SectionDto> sections;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SectionDto {
        public String name;
        public List<MatchDto> matches;
        public List<RankingDto> rankings;        // 순위별로 팀 목록이 나눠진 리스트
        public List<TeamDto> refinedRankings;    // 공동순위 처리 후 flatMap한 결과 리스트
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MatchDto {
        @JsonAlias({"id", "matchId"})
        public String matchId;
        public String state;   // unstarted, inProgress, completed, unneeded

        public List<String> previousMatchIds;
        public List<String> flags; // hasVod,
        public List<TeamDto> teams;

        // 프론트에 데이터 전송할 땐 matchId 사용
        @JsonProperty("matchId")
        public String getMatchId() {
            return matchId;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TeamDto {
        @JsonAlias({"id", "teamId"})
        private String teamId;  // JSON 입력과 출력을 모두 "id" 필드와 매핑
        public String slug;
        public String name;
        public String code;
        public String image;
        public int rank;
        public ResultDto result;    // matches
        public RecordDto record;    // rankings

        // JSON 출력 시 필드명 변경
        @JsonProperty("teamId")
        public String getTeamId() {
            return teamId;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ResultDto {
        public String outcome;
        public int gameWins;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecordDto {
        public int wins;
        public int losses;
        public int gameDiff;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RankingDto {
        public int ordinal;
        public List<TeamDto> teams;
    }

}
