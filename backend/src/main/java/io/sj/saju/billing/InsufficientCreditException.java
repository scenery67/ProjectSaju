package io.sj.saju.billing;

import java.util.UUID;

/** Thrown when a consume() would take the user's balance below zero. */
public class InsufficientCreditException extends RuntimeException {

    public InsufficientCreditException(UUID userAccountId) {
        super("insufficient credit for user account: " + userAccountId);
    }
}
