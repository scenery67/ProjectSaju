package io.sj.saju.attendance;

import java.util.UUID;

/** Thrown when an account tries to check in a second time on the same (KST) day. */
public class AlreadyCheckedInException extends RuntimeException {

    public AlreadyCheckedInException(UUID userAccountId) {
        super("already checked in today: " + userAccountId);
    }
}
