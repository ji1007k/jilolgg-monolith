package com.test.basic.lol.teams;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

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
        testTeam.setTeamCode("T1");
        testTeam.setTeamName("T1");
        testTeam.setSlug("t1"); // t1-challengers, t1-rookies, t1
        testTeam.setImage("http://static.lolesports.com/teams/1726801573959_539px-T1_2019_full_allmode.png");
        testTeam.setHomeLeague("LCK");
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
        assertThat(firstTeam.getTeamCode()).isEqualTo("T1");

        verify(teamRepository, times(1)).findAll();
    }

    @Test
    void testGetTeamByTeamCode() {
        String teamCode = testTeam.getTeamCode();

        // invocationOnMock: 호출된 메서드의 인자
        when(teamRepository.findByTeamCode(anyString())).thenAnswer(invocationOnMock -> {
            String requestedCode = invocationOnMock.getArgument(0);  // 검색할 teamCode
            Team mockTeam = new Team();
            mockTeam.setTeamCode(requestedCode);               // 요청받은 코드로 설정
            return Optional.of(mockTeam);                      // findByTeamCode는 Optional<Team> 리턴이니까!
        });

        // T1으로 호출한 결과가 T1인지 확인
        Team teamT1 = teamService.getTeamByTeamCodeFromDB(teamCode);
        assertThat(teamT1).isNotNull();
        assertThat(teamT1.getTeamCode()).isEqualTo("T1");

        // GEN으로 호출한 결과가 GEN인지 확인
        Team teamGen = teamService.getTeamByTeamCodeFromDB("GEN");
        assertThat(teamGen).isNotNull();
        assertThat(teamGen.getTeamCode()).isEqualTo("GEN");
    }

    @Test
    void testGetTeamByTeamCode_shouldThrow_WhenTeamDoesNotExist() {
        when(teamRepository.findByTeamCode(anyString())).thenReturn(Optional.empty());

        // 없는 팀 조회 시 에러 확인
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            teamService.getTeamByTeamCodeFromDB("DoesNotExist");
        });

        assertEquals("Team not found: DoesNotExist", exception.getMessage());
    }

}
