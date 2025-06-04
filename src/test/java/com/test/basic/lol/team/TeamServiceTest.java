package com.test.basic.lol.team;

import com.test.basic.lol.domain.team.Team;
import com.test.basic.lol.domain.team.TeamRepository;
import com.test.basic.lol.domain.team.TeamService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamService teamService;

    private Team testTeam;

    @BeforeEach
    void setup() {
        testTeam = new Team();
        testTeam.setCode("T1");
        testTeam.setName("T1");
        testTeam.setSlug("t1"); // t1-challengers, t1-rookies, t1
        testTeam.setImage("http://static.lolesports.com/teams/1726801573959_539px-T1_2019_full_allmode.png");
//        testTeam.setHomeLeague("LCK");
    }

    @Test
    void testGetAllTeams() {
        Team testTeam2 = new Team();
        List<Team> teams = List.of(testTeam, testTeam2);

        when(teamRepository.findAll()).thenReturn(teams);

        List<Team> result = teamService.getAllTeamsFromDB();
        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(1);

        Team firstTeam = result.get(0);
        assertThat(firstTeam.getCode()).isEqualTo("T1");

        verify(teamRepository, times(1)).findAll();
    }

    @Test
    void testGetTeamBySlug() {
        String slug = testTeam.getSlug();

        // invocationOnMock: 호출된 메서드의 인자
        when(teamRepository.findBySlug(anyString())).thenAnswer(invocationOnMock -> {
            String requestedSlug = invocationOnMock.getArgument(0);  // 검색할 code
            Team mockTeam = new Team();
            mockTeam.setSlug(requestedSlug);               // 요청받은 파리미터로 설정
            return Optional.of(mockTeam);                  // findBySlug는 Optional<Team> 리턴이니까!
        });

        // T1으로 호출한 결과가 T1인지 확인
        Team teamT1 = teamService.getTeamBySlugFromDB(slug);
        assertThat(teamT1).isNotNull();
        assertThat(teamT1.getSlug()).isEqualTo("t1");

        // GEN으로 호출한 결과가 GEN인지 확인
        Team teamGen = teamService.getTeamBySlugFromDB("geng");
        assertThat(teamGen).isNotNull();
        assertThat(teamGen.getSlug()).isEqualTo("geng");
    }

    @Test
    void testGetTeamBySlug_shouldThrow_WhenTeamDoesNotExist() {
        when(teamRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        // 없는 팀 조회 시 에러 확인
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            teamService.getTeamBySlugFromDB("DoesNotExist");
        });

        assertEquals("Team not found: DoesNotExist", exception.getMessage());
    }

}
