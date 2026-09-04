package io.sj.saju.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One admin-triggered mutation, for audit purposes. See V10 migration. */
@Entity
@Table(name = "admin_action_log")
public class AdminActionLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "admin_user_account_id")
    private UUID adminUserAccountId;

    @Column(name = "target_user_account_id")
    private UUID targetUserAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private AdminActionType actionType;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(nullable = false)
    private Instant createdAt;

    protected AdminActionLog() {
        // JPA
    }

    public AdminActionLog(
            UUID adminUserAccountId, UUID targetUserAccountId, AdminActionType actionType, String detail) {
        this.adminUserAccountId = adminUserAccountId;
        this.targetUserAccountId = targetUserAccountId;
        this.actionType = actionType;
        this.detail = detail;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAdminUserAccountId() {
        return adminUserAccountId;
    }

    public UUID getTargetUserAccountId() {
        return targetUserAccountId;
    }

    public AdminActionType getActionType() {
        return actionType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
