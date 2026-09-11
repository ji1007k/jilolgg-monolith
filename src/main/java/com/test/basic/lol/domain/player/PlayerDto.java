package com.test.basic.lol.domain.player;

import lombok.Data;

@Data
public class PlayerDto {
    private String playerId;
    private String name;
    private String role;
    private String image;
}
