package com.test.basic.lol.domain.league;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeagueService {

    private static final Logger logger = LoggerFactory.getLogger(LeagueService.class);

    private final LeagueRepository leagueRepository;
    private final UserLeagueOrderRepository userLeagueOrderRepository;
    private final LeagueMapper leagueMapper;


//    @Cacheable("leagues")
    public List<LeagueDto> getAllLeagues() {
        return leagueRepository.findAll().stream()
                .map(leagueMapper::entityToLeagueDto)
                .toList();
    }

    public List<LeagueDto> getAllLeagues(Long userId) {
        List<LeagueDto> allLeagues = getAllLeagues();
        
        // 로그인 안 한 경우(null) userId를 0으로 설정하여 기본 정렬 조회
        Long targetUserId = (userId == null) ? 0L : userId;

        List<UserLeagueOrder> orders = userLeagueOrderRepository.findByUserId(targetUserId);
        if (orders.isEmpty()) {
            // 0번 유저(기본 설정)도 없으면 그냥 기본 정렬 반환
            return allLeagues;
        }

        Map<String, Integer> orderMap = orders.stream()
                .collect(Collectors.toMap(UserLeagueOrder::getLeagueId, UserLeagueOrder::getDisplayOrder));

        return allLeagues.stream()
                .sorted(Comparator.comparingInt(league -> orderMap.getOrDefault(league.getLeagueId(), Integer.MAX_VALUE)))
                .toList();
    }

    @Transactional
    public void updateLeagueOrders(Long userId, List<String> leagueIds) {
        logger.info("Updating league orders for userId: {}, leagueIds: {}", userId, leagueIds);
        
        userLeagueOrderRepository.deleteByUserId(userId);
        logger.info("Deleted existing orders for userId: {}", userId);
        
        List<UserLeagueOrder> newOrders = new ArrayList<>();
        for (int i = 0; i < leagueIds.size(); i++) {
            newOrders.add(UserLeagueOrder.builder()
                    .userId(userId)
                    .leagueId(leagueIds.get(i))
                    .displayOrder(i)
                    .build());
        }
        userLeagueOrderRepository.saveAll(newOrders);
        logger.info("Saved {} new orders for userId: {}", newOrders.size(), userId);
    }

    public Optional<League> getLeagueByLeagueId(String leagueId) {
        return leagueRepository.findByLeagueId(leagueId);
    }
}
