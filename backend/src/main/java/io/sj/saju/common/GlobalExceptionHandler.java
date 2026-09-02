package io.sj.saju.common;

import io.sj.saju.billing.InsufficientCreditException;
import io.sj.saju.consultation.ConsultationFailedException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return errors;
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

    // LLM 호출 실패 — 소비된 크레딧은 ConsultationService가 이미 환급했다.
    @ExceptionHandler(ConsultationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleConsultationFailed(ConsultationFailedException ex) {
        return Map.of("error", "consultation_failed");
    }
}
