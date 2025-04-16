package com.test.basic.lol.teams;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<Team> getAllTeamsFromDB() {
        return teamRepository.findAll();
    }

    public Team getTeamByTeamCodeFromDB(String teamCode) {
        return teamRepository.findByTeamCode(teamCode)
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + teamCode));
    }

    /*public List<Team> getAllTeamsFromExternalApi() {
        
    }*/

}
