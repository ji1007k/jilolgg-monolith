package com.test.basic.lol.comp;

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
    private String outcome;

    public TeamMatchResult(String code, String outcome) {
        this.code = code;
        this.outcome = outcome;
    }


    @Override
    public String toString() {
        return "TeamMatchResult{code='" + code + "', outcome='" + outcome + "'}";
    }
}
