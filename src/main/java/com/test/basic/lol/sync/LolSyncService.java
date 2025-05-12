package com.test.basic.lol.sync;

import com.test.basic.lol.api.LolEsportsApiClient;
import com.test.basic.lol.leagues.League;
import com.test.basic.lol.leagues.LeagueDto;
import com.test.basic.lol.leagues.LeagueRepository;
import com.test.basic.lol.matches.Match;
import com.test.basic.lol.matches.MatchRepository;
import com.test.basic.lol.matches.MatchScheduleResponse;
import com.test.basic.lol.matchteams.MatchTeam;
import com.test.basic.lol.matchteams.MatchTeamRepository;
import com.test.basic.lol.teams.Team;
import com.test.basic.lol.teams.TeamRepository;
import com.test.basic.lol.tournaments.Tournament;
import com.test.basic.lol.tournaments.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

// TODO 일정 주기 간격으로 동기화 기능 구현 (배치)
@Service
public class LolSyncService {

    private static Logger logger = LoggerFactory.getLogger(LolSyncService.class);

    private final LolEsportsApiClient lolEsportsApiClient;
    private final LeagueRepository leagueRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchTeamRepository matchTeamRepository;

    public LolSyncService(LolEsportsApiClient lolEsportsApiClient, LeagueRepository leagueRepository, TournamentRepository tournamentRepository, MatchRepository matchRepository, TeamRepository teamRepository, MatchTeamRepository matchTeamRepository) {
        this.lolEsportsApiClient = lolEsportsApiClient;
        this.leagueRepository = leagueRepository;
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.matchTeamRepository = matchTeamRepository;
    }

    public void syncLeaguesFromLolEsportsApi() {
        Mono<List<LeagueDto>> leagueListMono = lolEsportsApiClient.fetchLeagues();

        StringBuilder sb = new StringBuilder();
        leagueListMono.subscribe(leagues -> {
            leagues.forEach(dto -> {
                Optional<League> existingLeague = leagueRepository.findByLeagueId(dto.getLeagueId());

                League league = existingLeague.orElse(new League(dto));
                leagueRepository.save(league);
                sb.append(String.format(">>> League synced: %s%n", dto.getLeagueId()));
            });
        }, error -> {
            throw new RuntimeException("Failed to sync leagues from LolEsports API", error);
        });

        if (sb.length() > 0) {
            logger.info(">>> 동기화 성공 리그 목록 :\n{}", sb);
        }
    }

    public void syncTournaments() {
        List<League> savedLeagues = leagueRepository.findAll();

        for (League league : savedLeagues) {
            lolEsportsApiClient.fetchTournamentsByLeagueId(league.getLeagueId())
                    .subscribe(tournaments -> {
                        tournaments.forEach(dto -> {
                            // 이미 존재하는 토너먼트인지 확인
                            Optional<Tournament> existingTournament = tournamentRepository.findByTournamentId(dto.getTournamentId());

                            Tournament tournament = existingTournament.orElse(new Tournament());
                            tournament.setTournamentId(dto.getTournamentId());
                            tournament.setSlug(dto.getSlug());
                            tournament.setStartDate(dto.getStartDate());
                            tournament.setEndDate(dto.getEndDate());
                            tournament.setLeague(league); // 연관관계 설정
                            tournamentRepository.save(tournament);
                        });
                    });
        }
    }

    public Mono<Void> syncMatchesByLeagueIds(List<String> leagueIds) {
        // 여러 개의 leagueId를 처리하는 Flux 생성
        return Flux.fromIterable(leagueIds)
                // 각 leagueId에 대해 syncMatchesByLeagueId 메서드를 비동기적으로 호출
                .flatMap(this::syncMatchesByLeagueId)
                .then();    // 모든 처리가 완료되면 완료된 Mono<Void>를 반환
    }

    public Mono<Void> syncMatchesByLeagueId(String leagueId) {
        // JPA 블로킹 작업을 안전하게 감싸기 위해 Mono.fromCallable 사용
        // Mono.fromCallable()은 블로킹 작업을 리액티브 체인에서 안전하게 감싸고, 비동기적으로 처리할 수 있게 도와줍니다.
        // 주어진 작업을 한 번만 실행하며, Mono.defer()와 달리 Callable 안에서 작업이 바로 실행됩니다.
        return Mono.fromCallable(() -> {
                    // 블로킹 JPA 코드: leagueId로 리그 정보를 조회
                    return leagueRepository.findByLeagueId(leagueId);
                })
                .subscribeOn(Schedulers.boundedElastic()) // 블로킹-safe 스레드에서 실행 (boundedElastic은 블로킹 작업을 처리할 수 있는 별도의 스레드 풀)
                .flatMap(leagueOpt -> {
                    // 리그가 없다면 에러 반환
                    if (leagueOpt.isEmpty()) {
                        return Mono.error(new RuntimeException("League not found with id: " + leagueId));
                    }

                    League league = leagueOpt.get();

                    // 경기 일정을 API에서 가져옴
                    return lolEsportsApiClient.fetchMatchSchedule(leagueId)
                            .flatMapMany(resp -> Flux.fromIterable(resp.getData().getSchedule().getEvents())) // 여러 개의 경기 이벤트 처리
                            .filter(event -> event.getMatch() != null) // 경기 정보가 있는 이벤트만 필터링
                            .publishOn(Schedulers.boundedElastic()) // 중간의 블로킹 작업도 안전하게 처리 (boundedElastic 스레드 풀에서 실행)
                            .flatMap(event -> {
                                // 경기에 대한 DTO 정보 받아오기
                                MatchScheduleResponse.MatchDto matchDto = event.getMatch();

                                // 기존 데이터라면 업데이트, 아니면 새로운 Match 객체 생성
                                Optional<Match> matchOpt = matchRepository.findByMatchId(matchDto.getId());
                                Match match = matchOpt.orElseGet(Match::new);

                                match.setMatchId(matchDto.getId());
                                match.setLeague(league);
                                match.setStartTime(OffsetDateTime.parse(event.getStartTime())
                                        .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                                        .toLocalDateTime());
                                match.setState(event.getState());
                                match.setBlockName(event.getBlockName());
                                match.setGameCount(matchDto.getStrategy().getCount());
                                match.setStrategy(matchDto.getStrategy().getType() + matchDto.getStrategy().getCount());

                                // Match를 저장하는 블로킹 작업을 안전하게 처리
                                return Mono.fromCallable(() -> matchRepository.save(match))
                                        .flatMap(savedMatch -> {
                                            // 경기 팀 정보를 저장하는 작업
                                            List<Mono<Void>> saveTeamMonos = matchDto.getTeams().stream()
                                                    .map(teamDto -> Mono.fromCallable(() ->
                                                                    teamRepository.findByCodeAndName(teamDto.getCode(), teamDto.getName()))
                                                            .filter(Optional::isPresent)  // 팀이 있으면
                                                            .map(Optional::get)
                                                            .flatMap(team -> Mono.fromCallable(() -> {
                                                                // MatchTeam 객체 생성 및 저장

                                                                Optional<MatchTeam> matchTeamOpt = matchTeamRepository
                                                                        .findByMatch_MatchIdAndTeam_TeamId(savedMatch.getMatchId(), team.getTeamId());
                                                                MatchTeam mTeam = matchTeamOpt.orElseGet(MatchTeam::new);

                                                                mTeam.setMatch(savedMatch);
                                                                mTeam.setTeam(team);
                                                                mTeam.setOutcome(teamDto.getResult().getOutcome());
                                                                mTeam.setGameWins(teamDto.getResult().getGameWins());
                                                                matchTeamRepository.save(mTeam); // 저장

                                                                return (Void) null;
                                                            }))
                                                    )
                                                    .toList(); // 모든 팀 저장 작업을 리스트로 모음

                                            // 여러 개의 Mono<Void>를 병렬로 실행하여 팀 저장 완료 후 반환
                                            return Flux.merge(saveTeamMonos).then(); // 모든 팀 저장 완료
                                        });
                            })
                            .then(); // 모든 이벤트 처리 후 완료
                });
    }

}
