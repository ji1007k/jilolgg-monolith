package com.test.basic.lol.batch;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueService;
import com.test.basic.lol.domain.match.MatchApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class MatchItemReader implements ItemReader<MatchEventWithLeague> {
    private static final Logger logger = LoggerFactory.getLogger(MatchItemReader.class);

    private final String leagueId;
    private final int targetYear;
    private final MatchApiService matchApiService;
    private final LeagueService leagueService;

    private Queue<MatchScheduleResponse.EventDto> buffer = new LinkedList<>();
    private String nextPageToken = null;
    private boolean finished = false;
    private boolean firstFetch = true;

    private League league;


    public MatchItemReader(String leagueId, int targetYear,
                           MatchApiService matchApiService,
                           LeagueService leagueService) {
        this.leagueId = leagueId;
        this.targetYear = targetYear;
        this.matchApiService = matchApiService;
        this.leagueService = leagueService;

        Optional<League> leagueOpt = leagueService.getLeagueByLeagueId(leagueId);
        if (leagueOpt.isEmpty()) {
            logger.warn("League not found with id: {}", leagueId);
            finished = true;
        } else {
            this.league = leagueOpt.get();
        }
    }


    // read 1번 -> EventDto 1개 반환
    @Override
    public MatchEventWithLeague read() {
        if (finished) {
            logger.debug("[Thread: {}] 읽기 종료", Thread.currentThread().getName());
            return null;
        }

        logger.debug("[Thread: {}] 읽기 실행", Thread.currentThread().getName());

        // 내부 버퍼링으로 fetch 최소화. (API 호출은 buffer가 비어 있을 때만)
        while (buffer.isEmpty()) {

            // 처음 호출도 아니고 nextPageToken도 없으면 종료
            if (!firstFetch && nextPageToken == null) {
                finished = true;
                return null;
            }

            MatchScheduleResponse response = matchApiService.fetchScheduleByLeagueIdAndPageToken(leagueId, nextPageToken);
            firstFetch = false; // 호출 이후 false로 설정

            if (response == null || response.getData() == null || response.getData().getSchedule() == null) {
//            logger.warn("[{}] 리그의 일정 정보가 비어 있습니다. nextToken: {}", leagueId, finalToken);
                finished = true;
                return null;
            }

            List<MatchScheduleResponse.EventDto> events = response.getData()
                    .getSchedule()
                    .getEvents()
                    .stream()
                    .filter(e -> e.getStartTime() != null)
                    .filter(e -> OffsetDateTime.parse(e.getStartTime())
                                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                                .toLocalDateTime().getYear() >= targetYear)
                    .toList();

            logger.debug("[Thread: {}] MatchIds: {}",
                    Thread.currentThread().getName(),
                    events.stream().map(event -> event.getMatch().getId())
            );

            // 갱신 대상 이벤트가 없으면 reader 종료
            if (events.isEmpty()) {
                finished = true;
                return null;
            }

            buffer.addAll(events);

            // 페이지 토큰 갱신
            nextPageToken = response.getData().getSchedule().getPages().getOlder();

            if (nextPageToken == null) {
                logger.debug("[Thread: {}] PageToken 없음", Thread.currentThread().getName());
                // 다음 read()에서 buffer 비면 종료되도록 유도
                finished = buffer.isEmpty();
            }
        }

        return new MatchEventWithLeague(buffer.poll(), league);
    }
}