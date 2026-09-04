package io.sj.saju.billing;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 관리자 조치 감사 로그. 실제 조치(AdminUserService/CreditService)가 끝난
 * 뒤 호출하는 게 원칙이다 — 가드 체크로 실패한 시도까지 "성공한 조치"처럼
 * 남기면 감사 로그의 의미가 없어진다. 단, DELETE_USER는 예외로, 대상 계정이
 * 삭제되기 *전에* 로그를 남겨야 target_user_account_id FK가 걸린다(삭제
 * 직후엔 그 id가 존재하지 않아 참조 자체가 불가능하다) — 삭제가 실제로
 * 일어나면 V10의 ON DELETE SET NULL이 이 로그 행의 참조만 알아서 끊는다.
 */
@Service
public class AdminActionLogService {

    private final AdminActionLogRepository repository;

    public AdminActionLogService(AdminActionLogRepository repository) {
        this.repository = repository;
    }

    public void log(UUID adminUserAccountId, UUID targetUserAccountId, AdminActionType actionType, String detail) {
        repository.save(new AdminActionLog(adminUserAccountId, targetUserAccountId, actionType, detail));
    }

    public List<AdminActionLog> recentLogs() {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }
}
