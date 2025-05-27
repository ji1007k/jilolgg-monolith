package com.test.basic.lol.domain.team;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public TeamSyncDto(String teamId, String code, String name, String slug, String image, String homeLeague) {
        this.teamId = teamId;
        this.code = code;
        this.name = name;
        this.slug = slug;
        this.image = image;
        this.homeLeague = homeLeague;
    }

}
