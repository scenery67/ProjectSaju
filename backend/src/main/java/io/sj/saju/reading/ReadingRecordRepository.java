package io.sj.saju.reading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, UUID> {
    List<ReadingRecord> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);

    // 일일 무료 한도 체크용 — 자정(KST) 이후 이 계정이 만든 기록 수를 센다.
    long countByUserAccountIdAndCreatedAtGreaterThanEqual(UUID userAccountId, Instant since);
}
