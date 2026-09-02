package io.sj.saju.consultation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, UUID> {
    List<ConsultationMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
