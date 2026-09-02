package io.sj.saju.reading;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, UUID> {
    List<ReadingRecord> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);
}
