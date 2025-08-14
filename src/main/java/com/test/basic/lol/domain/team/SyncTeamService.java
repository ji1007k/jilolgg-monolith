package com.test.basic.lol.domain.team;

import com.test.basic.lol.api.esports.LolEsportsApiClient;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SyncTeamService {
    private static final Logger logger = LoggerFactory.getLogger(SyncTeamService.class);

    private final TeamService teamService;

    private final LolEsportsApiClient apiClient;
    private final RedissonClient redissonClient;
    private RLock lock;

    @PersistenceContext
    private EntityManager entityManager;


    @CacheEvict(value = "teams", allEntries = true)
    public String syncTeamsFromLolEsportsApi() {
        // RLock은 자동적으로 락 타임아웃을 처리.
        // 락을 획득한 스레드가 락을 해제하지 않더라도, 일정 시간이 지나면 자동으로 락을 해제함
        lock = redissonClient.getLock("sync-teams-lock");
        boolean isLocked = false;

        try {
            long startTime = System.currentTimeMillis();  // 락 획득 시도 시작 시간 기록
            isLocked = lock.tryLock(1, TimeUnit.SECONDS); // 1초 대기
            long endTime = System.currentTimeMillis();  // 락 획득 시도 종료 시간 기록

            // 락 획득 시도 결과 로그
            logger.info(">>> 락 획득 결과: {}, 대기 시간: {}ms", isLocked, (endTime - startTime));

            if (isLocked) {
                logger.info(">>> 락 획득 성공. 팀 정보 수동 동기화 시작.");
            } else {
                logger.warn(">>> 락 획득 실패. 이미 다른 동기화 작업이 진행 중.");
                return "이미 동기화 작업이 진행 중입니다.";
            }

            // 락을 획득한 후 5초 대기 (테스트용)
            /*try {
                logger.info(">>> 락 획득 후 5초 대기 시작");
                Thread.sleep(5000); // 5초 대기
                logger.info(">>> 5초 대기 완료");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error(">>> 딜레이 중 예외 발생: {}", e.getMessage());
                return "딜레이 중 예외 발생";
            }*/

            // 실제 동기화 작업 실행
            logger.info(">>> LoL Esports API로부터 팀 정보 동기화 시작");
            long syncStartTime = System.currentTimeMillis();
            Mono<String> result = apiClient.fetchAllTeams();
            List<TeamSyncDto> externalTeams = teamService.parseTeamsFromResponse(result.block());

            int successCnt = 0;
            Map<String, StringBuilder> errorLogMap = new HashMap<>();
            for (TeamSyncDto dto : externalTeams) {
                try {
                    teamService.saveOrUpdate(dto);
                    successCnt++;
                } catch (Exception e) {
                    // 에러 메시지를 키로 사용
                    errorLogMap
                            .computeIfAbsent(e.getMessage(), k -> new StringBuilder())
                            .append(dto.getName()).append(", ");
                }
            }
            long syncEndTime = System.currentTimeMillis();

            // 에러 로그 출력
            if (!errorLogMap.isEmpty()) {
                StringBuilder groupedErrorLog = new StringBuilder(">>> 동기화 중 실패한 팀 목록:\n");
                errorLogMap.forEach((reason, names) -> {
                    if (names.length() >= 2) {
                        names.setLength(names.length() - 2);  // 마지막 ", " 제거
                    }

                    groupedErrorLog.append(String.format("원인: %s%n", reason));
                    groupedErrorLog.append("- ").append(names).append("\n");
                });
                logger.error("{}", groupedErrorLog);
            }

            logger.info(">>> 팀 동기화 완료 (성공 {}건 / 전체 {}건)", successCnt, externalTeams.size());
            logger.info(">>> LoL Esports API로부터 팀 정보 동기화 완료, 소요 시간: {}ms", (syncEndTime - syncStartTime));

            return "팀 수동 동기화 성공";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(">>> 락 획득 중 예외 발생: {}", e.getMessage());
            return "락 획득 실패";
        } finally {
            cleanup();
        }
    }

    // 애플리케이션이 종료되거나 컨테이너가 닫히기 전에 자원 정리
    @PreDestroy
    private void cleanup() {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            logger.warn(">>> 락 해제 완료");
        } else {
            logger.warn(">>> 락 해제 시도 없음 (락 획득 실패 또는 타임아웃에 의한 자동 해제)");
        }

        entityManager.clear();
    }
}
