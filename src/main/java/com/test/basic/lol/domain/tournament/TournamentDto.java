package com.test.basic.lol.domain.tournament;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/*{
    "id": "113503357263583149",
        "slug": "lck_split_3_2025",
        "startDate": "2025-07-23",
        "endDate": "2025-09-28"
}*/
@Data
@NoArgsConstructor
public class TournamentDto {

    @JsonProperty("id")
    private String tournamentId;
    private String slug;
    private LocalDate startDate;
    private LocalDate endDate;

    @Transient
    private boolean active;

    public TournamentDto(String tournamentId, String slug, LocalDate startDate, LocalDate endDate) {
        this.tournamentId = tournamentId;
        this.slug = slug;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
