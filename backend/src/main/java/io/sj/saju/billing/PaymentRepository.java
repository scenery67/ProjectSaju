package io.sj.saju.billing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);

    // 관리자 결제 내역 화면용 — 전체를 최신순으로 페이지네이션해서 본다.
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
