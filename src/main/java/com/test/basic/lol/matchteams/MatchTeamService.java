package com.test.basic.lol.matchteams;

import com.test.basic.lol.matches.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchTeamService {
    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;

    public List<String> findTeamIdsByLeagueId(String leagueId) {
        List<String> matchIds = matchRepository.findMatchIdByLeague_LeagueId(leagueId);
        return matchTeamRepository.findDistinctTeamIdByMatchIdIn(matchIds);
    }
}
