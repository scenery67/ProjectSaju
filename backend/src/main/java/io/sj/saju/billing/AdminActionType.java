package io.sj.saju.billing;

/** What kind of admin-triggered action was logged. See V10 migration. */
public enum AdminActionType {
    SET_ADMIN_TRUE,
    SET_ADMIN_FALSE,
    DELETE_USER,
    REFUND_PAYMENT,
    CREDIT_ADJUST,
    ANNOUNCEMENT
}
