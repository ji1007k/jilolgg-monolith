package com.test.basic.common.handler;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.List;

// 이전 테스트 결과로 변경된 DB 모든 테이블을 TRUNCATE로 초기화 (테스트 간 데이터 충돌 방지)
// 실제 통합테스트에 적합한 방식. (@Transactional은 보통 단위 테스트 시 사용)
// https://mangkyu.tistory.com/264
public class AcceptanceTestExecutionListener implements TestExecutionListener {

    // 테스트 메소드가 끝난 후 실행됨
    @Override
    public void afterTestMethod(TestContext testContext) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(testContext);
        List<String> truncateQueries = getTruncateQueries(jdbcTemplate);
        truncateTables(jdbcTemplate, truncateQueries);
    }

    // 테스트 메소드 실행 후 테이블 초기화
    // TABLE_SCHEMA = SCHEMA(): 현재 사용 중인 스키마를 자동으로 참조
    private List<String> getTruncateQueries(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList(
                "SELECT CONCAT('TRUNCATE TABLE ', TABLE_NAME, ';') AS q " +
                        "FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = SCHEMA()",
                String.class
        );
    }

    private JdbcTemplate getJdbcTemplate(TestContext testContext) {
        return testContext.getApplicationContext().getBean(JdbcTemplate.class);
    }

    private void truncateTables(JdbcTemplate jdbcTemplate, List<String> truncateQueries) {
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY FALSE");
        truncateQueries.forEach(query -> execute(jdbcTemplate, query));
        execute(jdbcTemplate, "SET REFERENTIAL_INTEGRITY TRUE");
    }

    private void execute(JdbcTemplate jdbcTemplate, String query) {
        jdbcTemplate.execute(query);
    }
}
