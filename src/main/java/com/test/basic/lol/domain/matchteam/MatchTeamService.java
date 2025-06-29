package com.test.basic.lol.domain.matchteam;

import com.test.basic.lol.domain.match.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchTeamService {
    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;

    public List<String> findTeamIdsByLeagueId(String leagueId) {
        List<String> matchIds = matchRepository.findMatchIdByLeague_LeagueId(leagueId);
        return matchTeamRepository.findDistinctTeamIdByMatchIdIn(matchIds);
    }

    public List<MatchTeam> getMatchByMatchIds(Set<String> matchIds) {
        return matchTeamRepository.findByMatch_MatchIdIn(matchIds);
    }

    public void saveMatchTeams(List<MatchTeam> matchTeamsToSave) {
        matchTeamRepository.saveAll(matchTeamsToSave);
    }

    public void deleteMatchTeams(List<MatchTeam> matchTeams) {
        matchTeamRepository.deleteAll(matchTeams);
    }
}
