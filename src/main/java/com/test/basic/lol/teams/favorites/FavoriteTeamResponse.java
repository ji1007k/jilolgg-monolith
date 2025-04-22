package com.test.basic.lol.teams.favorites;

import lombok.AllArgsConstructor;
import lombok.Data;

// 즐겨찾기 응답
@Data
@AllArgsConstructor
public class FavoriteTeamResponse {
    private Long teamId;
    private String teamCode;
    private String teamName;
    private Integer displayOrder;
}
