package com.test.basic.lol.api.esports.dto;

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

    @Getter
    @Setter
    @NoArgsConstructor
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
