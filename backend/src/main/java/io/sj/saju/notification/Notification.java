package io.sj.saju.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One inbox entry — 결제 완료, 출석 보너스, 관리자 공지 중 하나. */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_account_id", nullable = false)
    private UUID userAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(name = "credit_amount")
    private Integer creditAmount;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public Notification(UUID userAccountId, NotificationType type, String title, String body, Integer creditAmount) {
        this.userAccountId = userAccountId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.creditAmount = creditAmount;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        this.read = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Integer getCreditAmount() {
        return creditAmount;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
