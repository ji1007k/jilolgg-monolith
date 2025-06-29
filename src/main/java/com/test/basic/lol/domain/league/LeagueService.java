package com.test.basic.lol.domain.league;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final LeagueMapper leagueMapper;

    public LeagueService(LeagueRepository leagueRepository,
                         LeagueMapper leagueMapper) {
        this.leagueRepository = leagueRepository;
        this.leagueMapper = leagueMapper;
    }

    public List<LeagueDto> getAllLeagues() {
        return leagueRepository.findAll().stream()
                .map(leagueMapper::entityToLeagueDto)
                .toList();
    }

    public Optional<League> getLeagueByLeagueId(String leagueId) {
        return leagueRepository.findByLeagueId(leagueId);
    }
}
