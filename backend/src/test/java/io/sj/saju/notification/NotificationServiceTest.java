package io.sj.saju.notification;

import static org.assertj.core.api.Assertions.assertThat;

import io.sj.saju.auth.OAuthProvider;
import io.sj.saju.auth.UserAccount;
import io.sj.saju.auth.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "test-" + UUID.randomUUID(), "테스터"));
    }

    @Test
    void notifyCreatesAnUnreadEntryVisibleInList() {
        notificationService.notify(user.getId(), NotificationType.PAYMENT_COMPLETED, "충전 완료", "100 크레딧", 100);

        var list = notificationService.list(user.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).isRead()).isFalse();
        assertThat(notificationService.unreadCount(user.getId())).isEqualTo(1);
    }

    @Test
    void markAllReadClearsTheUnreadCountButKeepsTheEntries() {
        notificationService.notify(user.getId(), NotificationType.ATTENDANCE_BONUS, "출석 보너스", "3 크레딧", 3);
        notificationService.notify(user.getId(), NotificationType.PAYMENT_COMPLETED, "충전 완료", "100 크레딧", 100);

        notificationService.markAllRead(user.getId());

        assertThat(notificationService.unreadCount(user.getId())).isZero();
        assertThat(notificationService.list(user.getId())).hasSize(2);
    }

    @Test
    void broadcastAnnouncementNotifiesEveryExistingUser() {
        UserAccount other = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.GOOGLE, "test-" + UUID.randomUUID(), "다른 사용자"));

        int recipientCount = notificationService.broadcastAnnouncement("공지", "점검 안내");

        assertThat(recipientCount).isEqualTo(userAccountRepository.findAll().size());
        assertThat(notificationService.list(user.getId())).anyMatch(
                n -> n.getType() == NotificationType.ADMIN_ANNOUNCEMENT);
        assertThat(notificationService.list(other.getId())).anyMatch(
                n -> n.getType() == NotificationType.ADMIN_ANNOUNCEMENT);
    }
}
