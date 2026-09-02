package io.sj.saju.consultation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, UUID> {
    List<ConsultationSession> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);
}
