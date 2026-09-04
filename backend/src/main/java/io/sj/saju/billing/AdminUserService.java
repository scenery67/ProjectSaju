package io.sj.saju.billing;

import io.sj.saju.auth.UserAccount;
import io.sj.saju.auth.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 화면의 사용자 목록/권한 변경/탈퇴 처리. 결제·크레딧 조치는
 * CreditService가 이미 갖고 있어 여기서는 그걸 호출만 한다.
 */
@Service
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final CreditService creditService;
    private final AdminActionLogService adminActionLogService;

    // deleteById가 지우는 reading_record/consultation_* CASCADE는 Postgres가
    // DB 레벨에서 처리하는 거라 Hibernate 세션(1차 캐시)은 그 사실을 모른다 —
    // flush 없이는 삭제 SQL 자체가 아직 안 나갔을 수 있고, clear 없이는 이후
    // 같은 트랜잭션에서 그 자식 행을 다시 조회하면 이미 지워졌는데도 캐시된
    // 예전 값을 돌려줄 수 있다(CreditService의 raw-SQL 갱신 주변과 같은 문제).
    @PersistenceContext
    private EntityManager entityManager;

    public AdminUserService(
            UserAccountRepository userAccountRepository,
            CreditService creditService,
            AdminActionLogService adminActionLogService) {
        this.userAccountRepository = userAccountRepository;
        this.creditService = creditService;
        this.adminActionLogService = adminActionLogService;
    }

    /**
     * 검색어가 없으면 최근 가입한 50명만 보여준다 — 사용자가 많아지면 전체
     * 목록을 다 내려주는 게 감당이 안 되므로, 특정 사용자를 찾을 때는 검색을
     * 쓰게 한다. 검색어가 유효한 UUID면 정확히 그 계정만(id로), 아니면
     * 닉네임 부분 일치로 최대 50건을 찾는다.
     */
    public List<UserAccount> listUsers(String query) {
        if (query == null || query.isBlank()) {
            return userAccountRepository.findTop50ByOrderByCreatedAtDesc();
        }
        String trimmed = query.trim();
        try {
            UUID id = UUID.fromString(trimmed);
            return userAccountRepository.findById(id).map(List::of).orElseGet(List::of);
        } catch (IllegalArgumentException notAUuid) {
            return userAccountRepository.findTop50ByNicknameContainingIgnoreCaseOrderByCreatedAtDesc(trimmed);
        }
    }

    /** 본인의 관리자 권한은 스스로 해제할 수 없다 — 실수로 관리자가 아무도 없는 상태를 막는다. */
    @Transactional
    public void setAdmin(UUID targetUserAccountId, boolean admin, UUID actingAdminId) {
        if (!admin && targetUserAccountId.equals(actingAdminId)) {
            throw new IllegalArgumentException("본인 계정의 관리자 권한은 스스로 해제할 수 없어요");
        }
        UserAccount account = userAccountRepository.findById(targetUserAccountId)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + targetUserAccountId));
        account.setAdmin(admin);
        userAccountRepository.save(account);
        adminActionLogService.log(actingAdminId, targetUserAccountId,
                admin ? AdminActionType.SET_ADMIN_TRUE : AdminActionType.SET_ADMIN_FALSE, null);
    }

    /**
     * 계정 탈퇴 처리. 남은 크레딧을 먼저 환급 처리(원장에 남김)한 뒤 계정을
     * 삭제한다 — reading_record/consultation_*은 CASCADE로 함께 삭제되고,
     * payment/credit_transaction은 V8 마이그레이션 덕에 소유자만 NULL로
     * 바뀌며 회계 기록으로 남는다. 자기 자신은 탈퇴시킬 수 없다(관리자
     * 화면에서 스스로를 지워 잠기는 사고 방지).
     *
     * <p>감사 로그는 반드시 실제 삭제 *전에* 남긴다 — target_user_account_id
     * FK가 걸려 있어서, 계정이 없어진 뒤에는 그 id를 가리키는 새 로그 행을
     * 만들 수 없다(AdminActionLogService 클래스 주석 참고).
     */
    @Transactional
    public void deleteUser(UUID targetUserAccountId, UUID actingAdminId) {
        if (targetUserAccountId.equals(actingAdminId)) {
            throw new IllegalArgumentException("본인 계정은 이 화면에서 탈퇴 처리할 수 없어요");
        }
        UserAccount account = userAccountRepository.findById(targetUserAccountId)
                .orElseThrow(() -> new NoSuchElementException("user not found: " + targetUserAccountId));

        adminActionLogService.log(actingAdminId, targetUserAccountId, AdminActionType.DELETE_USER,
                "탈퇴 처리 (환급 전 잔여 크레딧 %d개)".formatted(account.getCreditBalance()));
        creditService.refundRemainingBalanceOnAccountDeletion(targetUserAccountId, actingAdminId);
        userAccountRepository.deleteById(targetUserAccountId);
        entityManager.flush();
        entityManager.clear();
    }
}
