package com.test.basic.lol.batch;

import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.teams.Team;
import com.test.basic.lol.teams.TeamRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TeamBatchService {
    private static final Logger logger = LoggerFactory.getLogger(TeamBatchService.class);

    private final TeamRepository teamRepository;
    private final LolEsportsApiClient apiClient;
    private final RedissonClient redissonClient;

    public TeamBatchService(TeamRepository teamRepository,
                            LolEsportsApiClient apiClient,
                            RedissonClient redissonClient) {
        this.teamRepository = teamRepository;
        this.apiClient = apiClient;
        this.redissonClient = redissonClient;
    }

    // RLock은 자동적으로 락 타임아웃을 처리. 
    // 락을 획득한 스레드가 락을 해제하지 않더라도, 일정 시간이 지나면 자동으로 락을 해제함
    @Transactional
    public String syncTeamsFromLolEsports() {
        RLock lock = redissonClient.getLock("sync-teams-lock");
        boolean isLocked = false;

        try {
            long startTime = System.currentTimeMillis();  // 락 획득 시도 시작 시간 기록
            isLocked = lock.tryLock(1, 600, TimeUnit.SECONDS); // 1초 대기 후 최대 6분 간 락 유지
            long endTime = System.currentTimeMillis();  // 락 획득 시도 종료 시간 기록

            // 락 획득 시도 결과 로그
            logger.info(">>> 락 획득 결과: {}, 대기 시간: {}ms", isLocked, (endTime - startTime));

            if (isLocked) {
                logger.info(">>> 락 획득 성공. 팀 정보 수동 동기화 시작.");
            } else {
                logger.warn(">>> 락 획득 실패. 이미 다른 동기화 작업이 진행 중.");
                return "이미 동기화 작업이 진행 중입니다.";
            }

            // 락을 획득한 후 5초 대기
            try {
                logger.info(">>> 락 획득 후 5초 대기 시작");
                Thread.sleep(5000); // 5초 대기
                logger.info(">>> 5초 대기 완료");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error(">>> 딜레이 중 예외 발생: {}", e.getMessage());
                return "딜레이 중 예외 발생";
            }

            // 실제 동기화 작업 실행
            logger.info(">>> LoL Esports API로부터 팀 정보 동기화 시작");
            long syncStartTime = System.currentTimeMillis();
            Mono<String> result = apiClient.fetchAllTeams();
            List<Team> externalTeams = apiClient.parseTeamsFromResponse(result.block());

            int successCnt = externalTeams.size();
            for (Team dto : externalTeams) {
                try {
                    saveOrUpdate(dto);
                } catch (Exception e) {
                    logger.error(">>> ❌ 팀 동기화 실패: {} - {}", dto.getTeamName(), e.getMessage());
                    successCnt--;
                }
            }
            long syncEndTime = System.currentTimeMillis();
            logger.info(">>> 팀 동기화 완료 ({}/{}개)", successCnt, externalTeams.size());
            logger.info(">>> LoL Esports API로부터 팀 정보 동기화 완료, 소요 시간: {}ms", (syncEndTime - syncStartTime));

            return "팀 수동 동기화 성공";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(">>> 락 획득 중 예외 발생: {}", e.getMessage());
            return "락 획득 실패";
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.warn(">>> 락 해제 완료");
            } else {
                logger.warn(">>> 락 해제 시도 없음 (락 획득 실패 또는 타임아웃에 의한 자동 해제)");
            }
        }
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
