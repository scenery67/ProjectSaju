package io.sj.saju.billing;

/** Thrown when Toss Payments rejects or fails to confirm a payment. */
public class TossPaymentFailedException extends RuntimeException {

    public TossPaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
