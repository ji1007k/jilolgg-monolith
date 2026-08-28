package com.test.basic.common.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// 전역(글로벌) 예외 처리 및 공통 설정을 담당하는 클래스에 붙여 사용하는 어노테이션
// Spring MVC의 예외 처리기(@ExceptionHandler)가 적용되는 영역
// 보통 컨트롤러 내부에서 발생한 예외를 처리
// Security 필터에서 이미 응답을 반환한 경우, @RestControllerAdvice는 실행X
// 인증/인가 외 에러 처리를 위한 핸들러
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 🔹 서비스가 의도적으로 지정한 상태코드를 그대로 내보낸다.
    // 이 핸들러가 없으면 아래 Exception 핸들러가 먼저 잡아 전부 500이 된다.
    // (@ExceptionHandler는 ResponseStatusExceptionResolver보다 먼저 동작한다)
    // 예: "동기화 작업이 실행 중입니다"(409), "X-Device-Id 헤더가 필요합니다"(400)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    // 🔹 400: 잘못된 요청 (유효성 검사 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request: " + e.getBindingResult().toString());
    }

    // 🔹 401: 인증 실패
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: " + e.getMessage());
    }

    // 🔹 403: 권한 부족
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: Access is denied");
    }

    // 🔹 404: 리소스 찾을 수 없음
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<String> handleNotFound(EmptyResultDataAccessException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found");
    }

    // 🔹 409: 데이터 무결성 위반 (중복 데이터 등)
    // 응답 본문에는 원인을 담지 않는다(제약조건 이름이 스키마를 노출한다).
    // 대신 반드시 로그로 남긴다 - 이걸 빠뜨려서 운영에서 로그인이 계속 409로
    // 실패하는데도 어떤 제약이 깨졌는지 알 수 없는 상태가 이어졌다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleIntegrityViolation(DataIntegrityViolationException e) {
        logger.error("데이터 무결성 위반 | 근본 원인: {}", rootCauseMessage(e), e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Cannot process request due to integrity violation");
    }

    /** JDBC 드라이버가 던진 실제 메시지(제약조건 이름·컬럼명)는 최하위 원인에 들어 있다. */
    private static String rootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    // 🔹 500: 서버 내부 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception e) {
        logger.error("처리되지 않은 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error: " + e.getMessage());
    }
}
