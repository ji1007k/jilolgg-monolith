package com.test.basic.lol.domain.match;

import com.test.basic.lol.domain.matchteam.MatchTeam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 대진 확정으로 밀려난 대진표 자리를 정리한다.
 *
 * 외부 API는 플레이오프·플레이-인의 대진표 자리와 실제 편성 경기에 서로 다른 matchId를 발급한다.
 * 대진이 확정되면 API 응답에서 자리 쪽은 사라지지만, 동기화가 삽입·갱신만 하고 삭제하지 않아
 * DB에 옛 자리가 남는다. 그 결과 같은 시각에 "TBD vs TBD"와 실제 경기가 함께 노출된다.
 *
 * <p><b>판정을 DB 내부만으로 한다.</b> "API 응답에 없으면 지운다"가 아니라
 * "같은 리그·같은 시각에 확정 경기가 있으면 자리를 감춘다"로 규칙을 세웠다.
 * 그래서 외부 API가 일시적으로 빈 응답을 주더라도 멀쩡한 경기가 사라지지 않는다.
 *
 * <p>행을 지우지 않고 표시만 바꾼다. 매 실행마다 다시 계산하므로
 * 더 이상 밀려나지 않은 경기는 저절로 복구된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceholderReconciler {

    /** 대진 확정 전 자리를 채워두는 팀. 코드는 TBDC지만 slug는 tbd다. */
    private static final String PLACEHOLDER_TEAM_SLUG = "tbd";

    static final String REASON_SUPERSEDED = "SUPERSEDED_PLACEHOLDER";

    private final MatchRepository matchRepository;

    /**
     * log  - 대상만 기록하고 데이터를 건드리지 않는다(기본값)
     * apply - 실제로 숨김 표시를 남긴다
     */
    @Value("${sync.placeholder-cleanup.mode:log}")
    private String mode;

    /**
     * @return 실제로 상태가 바뀐 건수. 0보다 크면 호출자가 캐시를 무효화해야 한다.
     */
    @Transactional
    public int reconcile() {
        List<Match> all = matchRepository.findAllForReconcile();
        if (all.isEmpty()) {
            return 0;
        }

        // 같은 리그·같은 시각에 확정 경기가 있는 슬롯
        Map<String, Boolean> slotHasRealMatch = new LinkedHashMap<>();
        for (Match match : all) {
            if (!isPlaceholder(match)) {
                slotHasRealMatch.put(slotKey(match), Boolean.TRUE);
            }
        }

        List<Match> toHide = new ArrayList<>();
        List<Match> toRestore = new ArrayList<>();

        for (Match match : all) {
            boolean shouldHide = isPlaceholder(match)
                    && Boolean.TRUE.equals(slotHasRealMatch.get(slotKey(match)));
            boolean isHidden = match.getHiddenAt() != null;

            if (shouldHide && !isHidden) {
                toHide.add(match);
            } else if (!shouldHide && isHidden && REASON_SUPERSEDED.equals(match.getHiddenReason())) {
                // 우리가 숨긴 것만 되돌린다. 다른 이유로 숨긴 건 건드리지 않는다.
                toRestore.add(match);
            }
        }

        if (toHide.isEmpty() && toRestore.isEmpty()) {
            log.info(">>> [플레이스홀더 정리] 대상 없음");
            return 0;
        }

        log.info(">>> [플레이스홀더 정리] mode={} 숨김대상={}건 복구대상={}건",
                mode, toHide.size(), toRestore.size());
        toHide.forEach(m -> log.info(">>>   숨김: matchId={} league={} startTime={}",
                m.getMatchId(), leagueId(m), m.getStartTime()));
        toRestore.forEach(m -> log.info(">>>   복구: matchId={}", m.getMatchId()));

        if (!"apply".equalsIgnoreCase(mode)) {
            log.info(">>> [플레이스홀더 정리] log 모드라 데이터를 변경하지 않았다. " +
                    "적용하려면 sync.placeholder-cleanup.mode=apply");
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        toHide.forEach(m -> {
            m.setHiddenAt(now);
            m.setHiddenReason(REASON_SUPERSEDED);
        });
        toRestore.forEach(m -> {
            m.setHiddenAt(null);
            m.setHiddenReason(null);
        });

        matchRepository.saveAll(toHide);
        matchRepository.saveAll(toRestore);

        int changed = toHide.size() + toRestore.size();
        log.info(">>> [플레이스홀더 정리] {}건 반영 완료", changed);
        return changed;
    }

    /** 참가 팀이 1개 이상이고 전부 TBD면 대진표 자리로 본다. */
    private boolean isPlaceholder(Match match) {
        List<MatchTeam> teams = match.getMatchTeams();

        // 팀이 아예 없는 경기는 자리가 아니라 팀 데이터가 유실된 실제 경기다.
        if (teams == null || teams.isEmpty()) {
            return false;
        }

        return teams.stream().allMatch(mt ->
                mt != null
                        && mt.getTeam() != null
                        && PLACEHOLDER_TEAM_SLUG.equalsIgnoreCase(mt.getTeam().getSlug()));
    }

    private String slotKey(Match match) {
        return leagueId(match) + "@" + Objects.toString(match.getStartTime(), "");
    }

    private String leagueId(Match match) {
        return match.getLeague() == null ? "" : match.getLeague().getLeagueId();
    }
}
