package com.test.basic.lol.domain.standings.dto;

import lombok.Data;

import java.util.List;

@Data
public class StageDto {
    private String id;      // 113503303283548977
    private String name;    // 정규 리그
    private String slug;    // regular_season
    private String type;
    private List<SectionDto> sections;
}
