package com.test.basic.lol.domain.team;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TeamSyncDto {
    @JsonProperty("id")
    private String teamId;
    private String code;
    private String name;
    private String slug;
    private String image;
    private String homeLeague;
    private List<PlayerSyncDto> players;

    public TeamSyncDto(String teamId, String code, String name, String slug, String image, String homeLeague) {
        this.teamId = teamId;
        this.code = code;
        this.name = name;
        this.slug = slug;
        this.image = image;
        this.homeLeague = homeLeague;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerSyncDto {
        private String id;
        private String name;
        private String role;
        private String image;
    }
}
