package io.sj.saju.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop50ByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);

    List<Notification> findByUserAccountIdAndReadFalse(UUID userAccountId);

    long countByUserAccountIdAndReadFalse(UUID userAccountId);
}
