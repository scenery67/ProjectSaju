package io.sj.saju.reading;

import java.util.UUID;

/** Thrown when a logged-in user has used up today's free reading quota and didn't opt to spend a credit. */
public class DailyReadingLimitExceededException extends RuntimeException {

    public DailyReadingLimitExceededException(UUID userAccountId) {
        super("daily free reading limit reached for user account: " + userAccountId);
    }
}
