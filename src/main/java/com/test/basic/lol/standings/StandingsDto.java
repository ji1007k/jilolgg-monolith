package com.test.basic.lol.standings;

import lombok.Data;

import java.util.List;

@Data
public class StandingsDto {
    private String id;      // tournamentId

    private List<StageDto> stages;
}
