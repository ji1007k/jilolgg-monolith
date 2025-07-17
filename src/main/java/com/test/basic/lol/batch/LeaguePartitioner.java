package com.test.basic.lol.batch;

import com.test.basic.lol.domain.league.League;
import com.test.basic.lol.domain.league.LeagueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@JobScope   // 기본 싱글톤 스코프에서는 JobParameters에 접근할 수 없어 명시적으로 설정
public class LeaguePartitioner implements Partitioner {

    private final LeagueRepository leagueRepository;

    @Value("#{jobParameters['targetYear']}")
    private String targetYear;

    /*private static final List<String> MAJOR_LEAGUE_IDS = List.of(
            // LCK, LCK CL
            "98767991310872058",
            "98767991335774713",
            // 국제 대회 (FIRST STAND, MSI, WORLDS)
            "113464388705111224",
            "98767991325878492",
            "98767975604431411",
            // LPL, LEC, LJL
            "98767991314006698",
            "98767991302996019",
            "98767991349978712");*/


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();

        List<String> leagueIds = leagueRepository.findAll()
                .stream()
                .map(League::getLeagueId)
                .toList();

        if (targetYear.isEmpty()) {
            targetYear = String.valueOf(Year.now().getValue());
        }

        for (int i = 0; i < leagueIds.size(); i++) {
            ExecutionContext context = new ExecutionContext();
            context.putString("leagueId", leagueIds.get(i));
            context.putString("targetYear", targetYear);
            partitions.put("partition_" + i, context);
        }

        return partitions;
    }
}
