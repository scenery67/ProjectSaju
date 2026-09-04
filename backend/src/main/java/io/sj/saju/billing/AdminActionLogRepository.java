package io.sj.saju.billing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, UUID> {
    List<AdminActionLog> findTop100ByOrderByCreatedAtDesc();
}
