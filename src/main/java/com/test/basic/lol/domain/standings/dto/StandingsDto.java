package com.test.basic.lol.domain.standings.dto;

import lombok.Data;

import java.util.List;

@Data
public class StandingsDto {
    private String id;      // tournamentId
    private List<StageDto> stages;
}
