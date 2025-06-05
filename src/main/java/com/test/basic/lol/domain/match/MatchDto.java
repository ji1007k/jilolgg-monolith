package com.test.basic.lol.domain.match;

import com.test.basic.lol.domain.matchteam.MatchTeamDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchDto {
    private String matchId;
    private String startTime;
    private String state;
    private String strategy;
    private String blockName;
    private String winningTeamCode;

    private List<MatchTeamDto> participants;
}
