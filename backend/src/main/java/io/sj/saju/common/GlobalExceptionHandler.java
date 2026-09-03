package io.sj.saju.common;

import io.sj.saju.attendance.AlreadyCheckedInException;
import io.sj.saju.billing.InsufficientCreditException;
import io.sj.saju.consultation.ConsultationFailedException;
import io.sj.saju.reading.DailyReadingLimitExceededException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// ResponseEntityExceptionHandler를 상속한다 — 안 그러면 아래 handleUnexpected
// (Exception.class 캐치올)가 잘못된 JSON 바디(HttpMessageNotReadableException,
// 원래 400), 지원 안 하는 메서드(HttpRequestMethodNotSupportedException, 원래
// 405) 같은 Spring MVC 표준 예외까지 전부 가로채서 500으로 뭉개버린다. 이
// 부모 클래스가 그런 프레임워크 예외들의 기본 상태코드 처리를 대신 해주고,
// 우리가 명시적으로 선언한 핸들러(아래)가 항상 우선한다.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // @ExceptionHandler가 아니라 override인 이유: MethodArgumentNotValidException은
    // ResponseEntityExceptionHandler 자신도 처리하는 표준 Spring MVC 예외라,
    // 여기서 또 @ExceptionHandler로 선언하면 같은 타입에 핸들러가 둘이 돼서
    // "Ambiguous @ExceptionHandler" 예외로 앱 자체가 뜨지 않는다. 부모가 이미
    // 갖고 있는 위임 지점을 오버라이드해서 응답 바디만 우리 형식으로 바꾼다.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return ResponseEntity.status(status).body(errors);
    }

    // 세션/기록을 찾을 수 없거나 이 사용자 소유가 아닌 경우 — 둘을 구분해
    // 알려주면 다른 사람의 리소스 존재 여부를 유추할 수 있어 그냥 404로 합친다.
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NoSuchElementException ex) {
        return Map.of("error", "not_found");
    }

    @ExceptionHandler(InsufficientCreditException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public Map<String, String> handleInsufficientCredit(InsufficientCreditException ex) {
        return Map.of("error", "insufficient_credit");
    }

    // 클라이언트 입력이 잘못된 경우(예: 관리자 화면에서 본인 권한을 스스로
    // 해제하려는 시도) — 메시지 자체가 사용자가 봐야 할 안내문이라 그대로 내려준다.
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("error", "invalid_request", "message", ex.getMessage());
    }

    @ExceptionHandler(AlreadyCheckedInException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleAlreadyCheckedIn(AlreadyCheckedInException ex) {
        return Map.of("error", "already_checked_in");
    }

    @ExceptionHandler(DailyReadingLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, String> handleDailyReadingLimitExceeded(DailyReadingLimitExceededException ex) {
        return Map.of("error", "daily_limit_reached");
    }

    // LLM 호출 실패 — 소비된 크레딧은 ConsultationService가 이미 환급했다.
    @ExceptionHandler(ConsultationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleConsultationFailed(ConsultationFailedException ex) {
        return Map.of("error", "consultation_failed");
    }

    // 위에서 못 잡은 나머지 전부 — Spring Boot 기본 에러 응답(include-stacktrace:
    // never)이 지금도 안전하긴 하지만, 그건 설정값에 기대는 안전망이라 나중에
    // 누가 디버깅용으로 그 설정을 바꾸고 안 되돌리면 그대로 새 나갈 수 있다.
    // 여기서 명시적으로 잡아 항상 일반화된 응답만 내려주고, 원인은 서버
    // 로그에만(사용자 응답에는 절대 노출하지 않고) 전체 스택과 함께 남긴다.
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        return Map.of("error", "internal_error");
    }
}
