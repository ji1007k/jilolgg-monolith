package com.test.basic.lol.batch;

import com.test.basic.lol.api.esports.dto.MatchScheduleResponse;
import com.test.basic.lol.domain.match.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;


/**
 * EventDto → 저장할 Entity 생성
 * Aggregate - 가공 완료된 도메인 묶음
 */
@RequiredArgsConstructor
public class MatchItemProcessor implements ItemProcessor<MatchEventWithLeague, MatchAggregate> {

    public MatchAggregate process(MatchEventWithLeague matchEventWithLeague) {
        MatchScheduleResponse.EventDto event = matchEventWithLeague.getEvent();

        if (event.getMatch() == null
                || event.getStartTime() == null)
            return null;

        LocalDateTime eventTime = OffsetDateTime
                .parse(event.getStartTime())
                .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();

        Match match = new Match();
        match.setMatchId(event.getMatch().getId());
        match.setLeague(matchEventWithLeague.getLeague());
        match.setStartTime(eventTime);
        match.setState(event.getState());
        match.setBlockName(event.getBlockName());
        match.setGameCount(event.getMatch().getStrategy().getCount());
        match.setStrategy(
                event.getMatch().getStrategy().getType() +
                event.getMatch().getStrategy().getCount()
        );

        MatchAggregate mag = new MatchAggregate(
                match,
                event.getMatch().getTeams()
        );

        return mag;
    }

}
