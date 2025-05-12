/*
package com.test.basic.lol;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2로 대체
class FavoriteTeamIntegrationTest {

    @Autowired
    private FavoriteTeamService favoriteTeamService;

    @Autowired
    private UserFavoriteTeamRepository favoriteRepo;

    @Autowired
    private TeamRepository teamRepo;

    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        // 테스트용 팀 3개 등록
        teamRepo.saveAll(List.of(
                new Team(10L, "팀 A"),
                new Team(20L, "팀 B"),
                new Team(30L, "팀 C")
        ));
    }

    @Test
    void 즐겨찾기_등록_조회_삭제_통합() {
        // 즐겨찾기 등록
        favoriteTeamService.addFavoriteTeam(userId, 10L);
        favoriteTeamService.addFavoriteTeam(userId, 20L);

        // 조회
        List<FavoriteTeamResponse> favorites = favoriteTeamService.getFavoriteTeams(userId);
        assertEquals(2, favorites.size());
        assertEquals(10L, favorites.get(0).getTeamId());

        // 삭제
        favoriteTeamService.removeFavoriteTeam(userId, 10L);
        List<FavoriteTeamResponse> afterDelete = favoriteTeamService.getFavoriteTeams(userId);
        assertEquals(1, afterDelete.size());
        assertEquals(20L, afterDelete.get(0).getTeamId());
    }

    @Test
    void 즐겨찾기_순서_변경_통합() {
        favoriteTeamService.addFavoriteTeam(userId, 10L);
        favoriteTeamService.addFavoriteTeam(userId, 20L);
        favoriteTeamService.addFavoriteTeam(userId, 30L);

        // 순서 변경: 30, 10, 20
        favoriteTeamService.updateFavoriteOrder(userId, List.of(30L, 10L, 20L));

        List<FavoriteTeamResponse> result = favoriteTeamService.getFavoriteTeams(userId);
        assertEquals(30L, result.get(0).getTeamId());
        assertEquals(10L, result.get(1).getTeamId());
        assertEquals(20L, result.get(2).getTeamId());
    }
}
*/
