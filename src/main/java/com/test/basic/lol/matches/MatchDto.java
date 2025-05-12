package com.test.basic.lol.matches;

import com.test.basic.lol.matchteams.MatchTeamDto;
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
    private String winningTeamCode;

    private List<MatchTeamDto> participants;
}
