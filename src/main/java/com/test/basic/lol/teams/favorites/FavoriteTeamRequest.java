package com.test.basic.lol.teams.favorites;


import lombok.Data;

// 즐겨찾기 등록 요청
@Data
public class FavoriteTeamRequest {
    private String teamCode;
    private String teamName;
}

