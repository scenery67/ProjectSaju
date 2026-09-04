package io.sj.saju.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/notifications/**는 permitAll 목록에 없어 SecurityConfig의
// anyRequest().authenticated()가 그대로 적용된다 — 로그인 필요.
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal UUID userAccountId) {
        return notificationService.list(userAccountId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal UUID userAccountId) {
        return new UnreadCountResponse(notificationService.unreadCount(userAccountId));
    }

    /** 알림함 화면을 열었을 때 호출 — 그 시점까지 쌓인 안읽음을 전부 읽음으로 바꾼다. */
    @PostMapping("/read-all")
    public void readAll(@AuthenticationPrincipal UUID userAccountId) {
        notificationService.markAllRead(userAccountId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType().name(), n.getTitle(), n.getBody(), n.getCreditAmount(), n.isRead(),
                n.getCreatedAt());
    }

    public record NotificationResponse(
            UUID id, String type, String title, String body, Integer creditAmount, boolean read,
            Instant createdAt) {
    }

    public record UnreadCountResponse(long count) {
    }
}
