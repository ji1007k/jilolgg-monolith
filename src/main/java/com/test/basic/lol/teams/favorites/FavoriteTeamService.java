package com.test.basic.lol.teams.favorites;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteTeamService {

    private final UserFavoriteTeamRepository repository;
//    private final TeamRepository teamRepository; // TODO 팀명, 팀코드 테이블 분리 후 팀 이름 조회

    public void addFavoriteTeam(Long userId, String teamCode) {
        if (repository.findByUserIdAndTeamCode(userId, teamCode).isPresent()) {
            throw new IllegalStateException("이미 즐겨찾기에 추가된 팀입니다.");
        }

        int order = repository.findByUserIdOrderByDisplayOrderAsc(userId).size(); // 제일 뒤에 추가
        repository.save(new UserFavoriteTeam(userId, teamCode, order));
    }

    public List<FavoriteTeamResponse> getFavoriteTeams(Long userId) {
        List<UserFavoriteTeam> favorites = repository.findByUserIdOrderByDisplayOrderAsc(userId);
        return favorites.stream()
                .map(fav -> new FavoriteTeamResponse(
                        fav.getTeamCode(),
                        fav.getTeamCode(),  // TODO 팀명 조회
//                        teamRepository.findNameByTeamCode(fav.getTeamCode()),
                        fav.getDisplayOrder()))
                .collect(Collectors.toList());
    }

    public void removeFavoriteTeam(Long userId, String teamCode) {
        repository.deleteByUserIdAndTeamCode(userId, teamCode);
    }

    public void updateFavoriteOrder(Long userId, List<String> orderedTeamCodes) {
        List<UserFavoriteTeam> favorites = repository.findByUserIdOrderByDisplayOrderAsc(userId);

        Map<String, UserFavoriteTeam> favMap = favorites.stream()
                .collect(Collectors.toMap(UserFavoriteTeam::getTeamCode, it -> it));

        for (int i = 0; i < orderedTeamCodes.size(); i++) {
            String teamCode = orderedTeamCodes.get(i);
            UserFavoriteTeam fav = favMap.get(teamCode);
            if (fav != null) {
                fav.updateOrder(i);
            }
        }

        repository.saveAll(favorites);
    }
}
