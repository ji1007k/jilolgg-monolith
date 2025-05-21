package com.test.basic.lol.api.dto.standings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StandingsResponse {

    private StandingsData data;

    @Data
    public static class StandingsData {
        private String tournamentId;      // tournamentId
        List<StandingsDto> standings;
    }

    @Data
    public static class StandingsDto {
        public List<StageDto> stages;
    }

    @Data
    public static class StageDto {
        public String id;
        public String name;
        public String type;
        public String slug;
        public List<SectionDto> sections;
    }

    @Data
    public static class SectionDto {
        public String name;
        public List<MatchDto> matches;
        public List<RankingDto> rankings;        // 순위별로 팀 목록이 나눠진 리스트
        public List<TeamDto> refinedRankings;    // 공동순위 처리 후 flatMap한 결과 리스트
    }

    @Data
    public static class MatchDto {
        @JsonProperty("id")
        public String matchId;
        public String state;   // unstarted, inProgress, completed, unneeded

        public List<String> previousMatchIds;
        public List<String> flags; // hasVod,
        public List<TeamDto> teams;

        // JSON 에서 받을 땐(역직렬화 시) id로 사용
        @JsonProperty("id")
        public void setMatchId(String id) {
            this.matchId = id;
        }

        // 프론트에 데이터 전송할 땐 matchId 사용
        @JsonProperty("matchId")
        public String getMatchId() {
            return matchId;
        }
    }

    @Data
    public static class TeamDto {
        // WRITE_ONLY: JSON에서 받을 때만 id로 사용 (역직렬화)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public String teamId;
        public String slug;
        public String name;
        public String code;
        public String image;
        public int rank;
        public ResultDto result;    // matches
        public RecordDto record;    // rankings

        // 프론트에 데이터 전송할 땐 teamId 사용
        @JsonProperty("id")
        public void setTeamId(String id) {
            this.teamId = id;
        }
    }

    @Data
    public static class ResultDto {
        public String outcome;
        public int gameWins;
    }


    @Data
    public static class RecordDto {
        public int wins;
        public int losses;
        public int gameDiff;
    }

    @Data
    public static class RankingDto {
        public int ordinal;
        public List<TeamDto> teams;
    }

}
