package com.test.basic.lol.domain.team;

import com.test.basic.lol.domain.player.PlayerDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class TeamDto {
    private String teamId;
    private String code;
    private String name;
    private String slug;
    private String image;
    private String leagueId;
    private List<PlayerDto> players;

    public TeamDto(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
