package com.test.basic.lol.domain.matchteam;

import com.test.basic.lol.domain.match.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void saveMatchTeams(List<MatchTeam> matchTeamsToSave) {
        for (MatchTeam matchTeam : matchTeamsToSave) {
            upsertMatchTeam(
                    matchTeam.getMatch().getMatchId(),
                    matchTeam.getTeam().getTeamId(),
                    matchTeam.getOutcome(),
                    matchTeam.getGameWins()
            );
        }
    }

    @Transactional
    public void upsertMatchTeam(String matchId, String teamId, String outcome, Integer gameWins) {
        matchTeamRepository.upsertByMatchIdAndTeamId(matchId, teamId, outcome, gameWins);
    }

    @Transactional
    public void deleteByMatchIds(Set<String> matchIds) {
        matchTeamRepository.deleteByMatch_MatchIdIn(matchIds);
    }
}
