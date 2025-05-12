/*package com.test.basic.lol;

@ExtendWith(MockitoExtension.class)
class FavoriteTeamServiceTest {

    @InjectMocks
    private FavoriteTeamService favoriteTeamService;

    @Mock
    private UserFavoriteTeamRepository favoriteRepo;

    @Mock
    private TeamRepository teamRepo;

    private final Long userId = 1L;
    private final Long teamId = 10L;

    @Test
    void 즐겨찾기_등록_성공() {
        // given
        when(favoriteRepo.findByUserIdAndTeamId(userId, teamId))
                .thenReturn(Optional.empty());
        when(favoriteRepo.findByUserIdOrderByDisplayOrderAsc(userId))
                .thenReturn(Collections.emptyList());

        // when
        favoriteTeamService.addFavoriteTeam(userId, teamId);

        // then
        verify(favoriteRepo).save(any(UserFavoriteTeam.class));
    }

    @Test
    void 즐겨찾기_중복_예외() {
        // given
        when(favoriteRepo.findByUserIdAndTeamId(userId, teamId))
                .thenReturn(Optional.of(new UserFavoriteTeam(userId, teamId, 0)));

        // when & then
        assertThrows(IllegalStateException.class, () -> {
            favoriteTeamService.addFavoriteTeam(userId, teamId);
        });
    }

    @Test
    void 즐겨찾기_조회_정상() {
        // given
        List<UserFavoriteTeam> mockList = List.of(
                new UserFavoriteTeam(userId, 10L, 0),
                new UserFavoriteTeam(userId, 20L, 1)
        );
        when(favoriteRepo.findByUserIdOrderByDisplayOrderAsc(userId))
                .thenReturn(mockList);
        when(teamRepo.findNameById(10L)).thenReturn("Team A");
        when(teamRepo.findNameById(20L)).thenReturn("Team B");

        // when
        List<FavoriteTeamResponse> result = favoriteTeamService.getFavoriteTeams(userId);

        // then
        assertEquals(2, result.size());
        assertEquals("Team A", result.get(0).getTeamName());
    }

    @Test
    void 즐겨찾기_삭제() {
        // when
        favoriteTeamService.removeFavoriteTeam(userId, teamId);

        // then
        verify(favoriteRepo).deleteByUserIdAndTeamId(userId, teamId);
    }

    @Test
    void 즐겨찾기_순서변경() {
        // given
        List<UserFavoriteTeam> mockList = List.of(
                new UserFavoriteTeam(userId, 10L, 0),
                new UserFavoriteTeam(userId, 20L, 1),
                new UserFavoriteTeam(userId, 30L, 2)
        );
        when(favoriteRepo.findByUserIdOrderByDisplayOrderAsc(userId))
                .thenReturn(mockList);

        List<Long> newOrder = List.of(30L, 10L, 20L);

        // when
        favoriteTeamService.updateFavoriteOrder(userId, newOrder);

        // then
        assertEquals(0, mockList.stream().filter(f -> f.getTeamId().equals(30L)).findFirst().get().getDisplayOrder());
        assertEquals(1, mockList.stream().filter(f -> f.getTeamId().equals(10L)).findFirst().get().getDisplayOrder());
        assertEquals(2, mockList.stream().filter(f -> f.getTeamId().equals(20L)).findFirst().get().getDisplayOrder());
        verify(favoriteRepo).saveAll(anyList());
    }
}*/
