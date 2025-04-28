package com.test.basic.lol.matches;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class TeamMatchResult {
    @Id
    @GeneratedValue
    private UUID id;
    private String code;
    private String name;
    private String outcome;

    public TeamMatchResult(String code, String name, String outcome) {
        this.code = code;
        this.name = name;
        this.outcome = outcome;
    }

    @Override
    public String toString() {
        return "TeamMatchResult{code='" + code + "', name='" + name + "', outcome='" + outcome + "'}";
    }
}
