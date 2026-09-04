package io.sj.saju.billing;

import io.sj.saju.auth.UserAccount;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 결제/크레딧 조치 API. SecurityConfig에서 /api/admin/**은
 * ROLE_ADMIN만 통과하도록 이미 막아뒀다 — 여기서는 인가가 됐다는 전제로 동작한다.
 * "누가 결제했는지 보고, 환불/수동 지급 조치를 할 수 있어야 한다"(2026-09-01
 * 요구사항)에 대응. 상담(채팅) 로그 열람은 LLM 상담 기능 자체가 아직 없어
 * 이 컨트롤러에는 없다 — 그 기능이 생기면 여기에 조회 엔드포인트를 추가한다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CreditService creditService;
    private final AdminUserService adminUserService;
    private final AdminActionLogService adminActionLogService;
    private final PaymentRepository paymentRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    public AdminController(
            CreditService creditService,
            AdminUserService adminUserService,
            AdminActionLogService adminActionLogService,
            PaymentRepository paymentRepository,
            CreditTransactionRepository creditTransactionRepository) {
        this.creditService = creditService;
        this.adminUserService = adminUserService;
        this.adminActionLogService = adminActionLogService;
        this.paymentRepository = paymentRepository;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    /** 관리자 조치 감사 로그 — 최근 100건. */
    @GetMapping("/action-logs")
    public List<ActionLogResponse> actionLogs() {
        return adminActionLogService.recentLogs().stream().map(this::toResponse).toList();
    }

    /**
     * 사용자 목록 — user_account_id를 화면에서 직접 확인/복사할 수 있게 한다.
     * query 없으면 최근 가입 50명, 있으면 닉네임 부분일치 또는 정확한 id로 찾는다.
     */
    @GetMapping("/users")
    public List<UserResponse> users(@RequestParam(required = false) String query) {
        return adminUserService.listUsers(query).stream().map(this::toResponse).toList();
    }

    /** 관리자 권한 부여/해제. */
    @PostMapping("/users/{userAccountId}/admin")
    public void setAdmin(
            @AuthenticationPrincipal UUID adminUserAccountId,
            @PathVariable UUID userAccountId,
            @RequestBody SetAdminRequest request) {
        adminUserService.setAdmin(userAccountId, request.admin(), adminUserAccountId);
    }

    /** 계정 탈퇴 — 남은 크레딧은 환급 처리 후 계정을 삭제한다. */
    @DeleteMapping("/users/{userAccountId}")
    public void deleteUser(
            @AuthenticationPrincipal UUID adminUserAccountId, @PathVariable UUID userAccountId) {
        adminUserService.deleteUser(userAccountId, adminUserAccountId);
    }

    /** 전체 결제 내역 — 누가(userAccountId) 얼마를 언제 결제했는지 최신순. */
    @GetMapping("/payments")
    public List<PaymentResponse> payments(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 특정 사용자의 크레딧 증감 원장 — 결제/환불/상담 소모가 언제 어떻게 있었는지 감사용. */
    @GetMapping("/users/{userAccountId}/transactions")
    public List<TransactionResponse> userTransactions(@PathVariable UUID userAccountId) {
        return creditTransactionRepository.findByUserAccountIdOrderByCreatedAtDesc(userAccountId).stream()
                .map(t -> new TransactionResponse(
                        t.getId(), t.getType().name(), t.getAmount(), t.getBalanceAfter(), t.getNote(), t.getCreatedAt()))
                .toList();
    }

    @PostMapping("/payments/{paymentId}/refund")
    public PaymentResponse refund(
            @AuthenticationPrincipal UUID adminUserAccountId,
            @PathVariable UUID paymentId,
            @RequestBody RefundRequest request) {
        Payment payment = creditService.refund(paymentId, adminUserAccountId, request.reason());
        return toResponse(payment);
    }

    /** 크레딧을 수동으로 더 넣어주거나(양수) 회수(음수)한다 — 보상/보정 조치용. */
    @PostMapping("/users/{userAccountId}/credit-adjust")
    public void adjustCredit(
            @AuthenticationPrincipal UUID adminUserAccountId,
            @PathVariable UUID userAccountId,
            @RequestBody CreditAdjustRequest request) {
        creditService.adminAdjust(userAccountId, request.amount(), adminUserAccountId, request.reason());
    }

    private ActionLogResponse toResponse(AdminActionLog l) {
        return new ActionLogResponse(
                l.getId(), l.getAdminUserAccountId(), l.getTargetUserAccountId(), l.getActionType().name(),
                l.getDetail(), l.getCreatedAt());
    }

    private UserResponse toResponse(UserAccount u) {
        return new UserResponse(
                u.getId(), u.getProvider().name(), u.getNickname(), u.getCreditBalance(), u.isAdmin(),
                u.getCreatedAt(), u.getLastLoginAt());
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getUserAccountId(), p.getCreditAmount(), p.getAmountKrw(), p.getStatus().name(),
                p.getPgProvider(), p.getPgTransactionId(), p.getCreatedAt(), p.getCompletedAt(),
                p.getRefundedAt(), p.getRefundedBy(), p.getRefundReason());
    }

    public record RefundRequest(String reason) {
    }

    public record CreditAdjustRequest(int amount, String reason) {
    }

    public record SetAdminRequest(boolean admin) {
    }

    public record UserResponse(
            UUID id, String provider, String nickname, int creditBalance, boolean isAdmin,
            Instant createdAt, Instant lastLoginAt) {
    }

    public record TransactionResponse(
            UUID id, String type, int amount, int balanceAfter, String note, Instant createdAt) {
    }

    public record PaymentResponse(
            UUID id, UUID userAccountId, int creditAmount, int amountKrw, String status,
            String pgProvider, String pgTransactionId, Instant createdAt, Instant completedAt,
            Instant refundedAt, UUID refundedBy, String refundReason) {
    }

    public record ActionLogResponse(
            UUID id, UUID adminUserAccountId, UUID targetUserAccountId, String actionType, String detail,
            Instant createdAt) {
    }
}
