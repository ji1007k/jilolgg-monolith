package com.test.basic.lol.batch;

import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.teams.Team;
import com.test.basic.lol.teams.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TeamBatchService {
    private static final Logger logger = LoggerFactory.getLogger(TeamBatchService.class);

    private final TeamRepository teamRepository;
    private final LolEsportsApiClient apiClient;

    public TeamBatchService(TeamRepository teamRepository,
                            LolEsportsApiClient apiClient) {
        this.teamRepository = teamRepository;
        this.apiClient = apiClient;
    }

    @Transactional
    public void syncTeamsFromLolEsports() {
        List<Team> externalTeams = apiClient.fetchAllTeams();

        for (Team dto : externalTeams) {
            try {
                saveOrUpdate(dto);
            } catch (Exception e) {
                logger.error("❌ 팀 동기화 실패: {} - {}", dto.getTeamName(), e.getMessage());
            }
        }

        logger.info("✔ 전체 팀 동기화 완료 (총 {}개)", externalTeams.size());
    }


    public void saveOrUpdate(Team dto) {
        Optional<Team> existing = teamRepository.findBySlug(dto.getSlug());

        if (existing.isPresent()) {
            // 이미 존재하면 업데이트
            Team team = existing.get();
            team.setTeamName(dto.getTeamName());
            team.setSlug(dto.getSlug());
            team.setImage(dto.getImage());
            team.setHomeLeague(dto.getHomeLeague());
            teamRepository.save(team);
        } else {
            // 존재하지 않으면 새로 저장
            Team newTeam = new Team(
                    dto.getTeamCode(),
                    dto.getTeamName(),
                    dto.getSlug(),
                    dto.getImage(),
                    dto.getHomeLeague()
            );
            teamRepository.save(newTeam);
        }
    }


}
