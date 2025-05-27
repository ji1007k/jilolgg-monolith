package com.test.basic.lol.domain.team;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TeamDto {
    private String teamId;
    private String code;
    private String name;
    private String slug;
    private String image;
    private String leagueId;

    public TeamDto(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
