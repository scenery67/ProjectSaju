package io.sj.saju.notification;

import io.sj.saju.auth.UserAccount;
import io.sj.saju.auth.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    public NotificationService(
            NotificationRepository notificationRepository, UserAccountRepository userAccountRepository) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void notify(UUID userAccountId, NotificationType type, String title, String body, Integer creditAmount) {
        notificationRepository.save(new Notification(userAccountId, type, title, body, creditAmount));
    }

    /** 관리자 공지 — 현재 가입한 모든 사용자에게 한 건씩 발송한다. */
    @Transactional
    public int broadcastAnnouncement(String title, String body) {
        List<UUID> userAccountIds = userAccountRepository.findAll().stream().map(UserAccount::getId).toList();
        userAccountIds.forEach(id -> notify(id, NotificationType.ADMIN_ANNOUNCEMENT, title, body, null));
        return userAccountIds.size();
    }

    public List<Notification> list(UUID userAccountId) {
        return notificationRepository.findTop50ByUserAccountIdOrderByCreatedAtDesc(userAccountId);
    }

    public long unreadCount(UUID userAccountId) {
        return notificationRepository.countByUserAccountIdAndReadFalse(userAccountId);
    }

    @Transactional
    public void markAllRead(UUID userAccountId) {
        List<Notification> unread = notificationRepository.findByUserAccountIdAndReadFalse(userAccountId);
        unread.forEach(Notification::markRead);
        notificationRepository.saveAll(unread);
    }
}
