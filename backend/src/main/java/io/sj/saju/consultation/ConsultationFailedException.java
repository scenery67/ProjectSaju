package io.sj.saju.consultation;

/** LLM call failed (missing key, timeout, upstream error) — the consumed credit is refunded before this is thrown. */
public class ConsultationFailedException extends RuntimeException {

    public ConsultationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
