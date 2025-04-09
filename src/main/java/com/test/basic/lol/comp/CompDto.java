package com.test.basic.lol.comp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompDto {
    private String startTime;
    private String state;
    private List<String> teams;
}
