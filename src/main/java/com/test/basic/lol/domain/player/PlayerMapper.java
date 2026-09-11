package com.test.basic.lol.domain.player;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlayerMapper {
    PlayerDto playerToPlayerDto(Player player);

    List<PlayerDto> playersToPlayerDtos(List<Player> players);
}
