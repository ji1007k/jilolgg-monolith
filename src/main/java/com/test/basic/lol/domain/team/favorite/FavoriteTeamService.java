package com.test.basic.lol.domain.team.favorite;

import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteTeamService {

    private final UserFavoriteTeamRepository repository;
    private final TeamRepository teamRepository;

    @Transactional
    public void addFavoriteTeam(Long userId, String teamId) {
        if (repository.findByUserIdAndTeam_TeamId(userId, teamId).isPresent()) {
            throw new IllegalStateException("이미 즐겨찾기에 추가된 팀입니다.");
        }

        Optional<Team> team = teamRepository.findByTeam_TeamId(teamId);
        if (! team.isPresent()) {
            throw new EntityNotFoundException("존재하지 않는 팀입니다.");
        }

        Integer maxOrder = repository.findMaxDisplayOrderByUserId(userId);
        int order = (maxOrder == null) ? 0 : maxOrder + 1;
        repository.save(new UserFavoriteTeam(userId, order, team.get()));
    }

    public List<FavoriteTeamResponse> getFavoriteTeams(Long userId) {
        List<FavoriteTeamResponse> favorites = repository.findFavoriteTeamsByUserIdOrderByDisplayOrderDesc(userId);
        return favorites;
    }

    @Transactional
    public void removeFavoriteTeam(Long userId, String teamId) {
        repository.deleteByUserIdAndTeam_TeamId(userId, teamId);
    }

    @Transactional
    public void updateFavoriteOrder(Long userId, List<Long> orderedTeamIds) {
        List<UserFavoriteTeam> favorites = repository.findByUserIdOrderByDisplayOrderDesc(userId);

        Map<Long, UserFavoriteTeam> favMap = favorites.stream()
                .collect(Collectors.toMap(fav -> fav.getTeam().getId(), it -> it));

        for (int i = 0; i < orderedTeamIds.size(); i++) {
            Long teamId = orderedTeamIds.get(i);
            UserFavoriteTeam fav = favMap.get(teamId);
            if (fav != null) {
                fav.updateOrder(i);
            }
        }

        repository.saveAll(favorites);
    }
}
