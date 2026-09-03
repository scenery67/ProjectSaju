package io.sj.saju.attendance;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceCheckRepository extends JpaRepository<AttendanceCheck, UUID> {
    boolean existsByUserAccountIdAndCheckedDate(UUID userAccountId, LocalDate checkedDate);

    Optional<AttendanceCheck> findByUserAccountIdAndCheckedDate(UUID userAccountId, LocalDate checkedDate);
}
