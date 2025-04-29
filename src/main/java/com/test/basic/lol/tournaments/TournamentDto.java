package com.test.basic.lol.tournaments;

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
    private String id;
    private String slug;
    private LocalDate startDate;
    private LocalDate endDate;
}
