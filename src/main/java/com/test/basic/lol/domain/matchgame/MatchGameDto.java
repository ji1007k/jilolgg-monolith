package com.test.basic.lol.domain.matchgame;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchGameDto {
    private Integer number;
    private String state;
    private String blueTeamId;
    private String redTeamId;
    /** 확정되지 않았으면 null */
    private String winnerTeamId;
}
