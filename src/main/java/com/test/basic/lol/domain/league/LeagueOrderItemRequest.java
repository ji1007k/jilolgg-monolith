package com.test.basic.lol.domain.league;

public record LeagueOrderItemRequest(
        String leagueId,
        boolean hidden
) {
}
