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
//@RequiredArgsConstructor
public class TeamInfo {
    @Id
    @GeneratedValue
    private UUID id;
    private String code;
    private String outcome;

    public TeamInfo(String code, String outcome) {
        this.code = code;
        this.outcome = outcome;
    }


    @Override
    public String toString() {
        return "TeamInfo{code='" + code + "', outcome='" + outcome + "'}";
    }
}
