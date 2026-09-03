package io.sj.saju.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One day's attendance check-in for one account. See V9 migration. */
@Entity
@Table(name = "attendance_check", uniqueConstraints = @UniqueConstraint(columnNames = {"user_account_id", "checked_date"}))
public class AttendanceCheck {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_account_id", nullable = false)
    private UUID userAccountId;

    @Column(name = "checked_date", nullable = false)
    private LocalDate checkedDate;

    @Column(name = "streak_count", nullable = false)
    private int streakCount;

    @Column(nullable = false)
    private Instant createdAt;

    protected AttendanceCheck() {
        // JPA
    }

    public AttendanceCheck(UUID userAccountId, LocalDate checkedDate, int streakCount) {
        this.userAccountId = userAccountId;
        this.checkedDate = checkedDate;
        this.streakCount = streakCount;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public LocalDate getCheckedDate() {
        return checkedDate;
    }

    public int getStreakCount() {
        return streakCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
