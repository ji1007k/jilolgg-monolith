package com.test.basic.lol.batch;

import com.test.basic.lol.domain.league.LeagueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LeaguePartitioner implements Partitioner {

    private final LeagueRepository leagueRepository;

    private static final List<String> MAJOR_LEAGUE_IDS = List.of(
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
            "98767991349978712");


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
//        List<League> leagues = leagueRepository.findAll();

        for (int i = 0; i < MAJOR_LEAGUE_IDS.size(); i++) {
            ExecutionContext context = new ExecutionContext();
            context.putString("leagueId", MAJOR_LEAGUE_IDS.get(i));
            context.putInt("targetYear", Year.now().getValue());
            partitions.put("partition_" + i, context);
        }

        return partitions;
    }
}
