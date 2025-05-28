package com.test.basic.lol.batch;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import com.test.basic.lol.domain.match.MatchApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


@RequiredArgsConstructor
public class MatchItemReader implements ItemReader<MatchEventWithLeague> {
    private final String leagueId;
    private final int targetYear;
    private final MatchApiService matchApiService;
    private final LeagueRepository leagueRepository;

    private Queue<MatchScheduleResponse.EventDto> buffer = new LinkedList<>();
    private String nextPageToken = null;
    private boolean finished = false;

    private final League league;


    public MatchItemReader(String leagueId, int targetYear,
                           MatchApiService matchApiService,
                           LeagueRepository leagueRepository) {
        this.leagueId = leagueId;
        this.targetYear = targetYear;
        this.matchApiService = matchApiService;
        this.leagueRepository = leagueRepository;

        this.league = leagueRepository.findByLeagueId(leagueId)
                .orElseThrow(() -> new RuntimeException("League not found with id: " + leagueId));
    }


    // read 1번 -> EventDto 1개 반환
    @Override
    public MatchEventWithLeague read() {
        if (finished) return null;

        // 내부 버퍼링으로 fetch 최소화. (API 호출은 buffer가 비어 있을 때만)
        while (buffer.isEmpty()) {
            MatchScheduleResponse response = matchApiService.fetchScheduleByLeagueIdAndPageToken(leagueId, nextPageToken);
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

            // 갱신 대상 이벤트가 없으면 reader 종료
            if (events.isEmpty()) {
                finished = true;
                return null;
            }

            buffer.addAll(events);
            nextPageToken = response.getData().getSchedule().getPages().getOlder();

            if (nextPageToken == null && buffer.isEmpty()) {
                finished = true;
            }
        }

        return new MatchEventWithLeague(buffer.poll(), league);
    }
}