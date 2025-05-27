package com.test.basic.lol.domain.team;

import lombok.Data;

@Data
public class TournamentTeamRankingDto {
    private String teamId;
    private String code;
    private String name;
    private String slug;
    private String image;

//    @Transient
    private String rank;
//    @Transient
    private String record; // "8,0,10" => 순서대로 win,losses,gameDiff count. 승리/패배 경기수, 득실차
}
