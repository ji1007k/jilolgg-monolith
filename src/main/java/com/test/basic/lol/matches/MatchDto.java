package com.test.basic.lol.matches;

import com.test.basic.lol.teams.Team;
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
    private String startTime;
    private String state;
    private String winningTeamCode;
    private List<Team> participants;
}
