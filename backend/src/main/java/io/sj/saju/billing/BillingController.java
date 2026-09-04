package io.sj.saju.billing;

import io.sj.saju.auth.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 사용자 본인의 크레딧/결제 화면용 API. 마이페이지의
 * "보유 크레딧/충전하기/결제내역"에 대응한다. purchases로 PENDING 결제
 * 레코드를 만든 뒤, 프론트가 토스 결제창을 띄우고 돌아오면 confirm으로
 * 실제 승인을 확인한다 — 검증 로직 자체는 CreditService.confirmTossPurchase에
 * 모아뒀다(서비스 계층에서 테스트하기 위해, 이 컨트롤러는 얇게 유지).
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final CreditService creditService;
    private final CreditPackageRepository creditPackageRepository;
    private final PaymentRepository paymentRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final UserAccountRepository userAccountRepository;

    public BillingController(
            CreditService creditService,
            CreditPackageRepository creditPackageRepository,
            PaymentRepository paymentRepository,
            CreditTransactionRepository creditTransactionRepository,
            UserAccountRepository userAccountRepository) {
        this.creditService = creditService;
        this.creditPackageRepository = creditPackageRepository;
        this.paymentRepository = paymentRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/packages")
    public List<PackageResponse> packages() {
        return creditPackageRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(p -> new PackageResponse(p.getId(), p.getName(), p.getCreditAmount(), p.getPriceKrw()))
                .toList();
    }

    @GetMapping("/me")
    public ResponseEntity<BalanceResponse> me(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return userAccountRepository.findById(userAccountId)
                .map(account -> ResponseEntity.ok(new BalanceResponse(account.getCreditBalance())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(@AuthenticationPrincipal UUID userAccountId) {
        return creditTransactionRepository.findByUserAccountIdOrderByCreatedAtDesc(userAccountId).stream()
                .map(t -> new TransactionResponse(
                        t.getId(), t.getType().name(), t.getAmount(), t.getBalanceAfter(), t.getNote(), t.getCreatedAt()))
                .toList();
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments(@AuthenticationPrincipal UUID userAccountId) {
        return paymentRepository.findByUserAccountIdOrderByCreatedAtDesc(userAccountId).stream()
                .map(p -> new PaymentResponse(
                        p.getId(), p.getCreditAmount(), p.getAmountKrw(), p.getStatus().name(), p.getCreatedAt()))
                .toList();
    }

    @PostMapping("/purchases")
    public PaymentResponse createPurchase(
            @AuthenticationPrincipal UUID userAccountId, @RequestBody CreatePurchaseRequest request) {
        Payment payment = creditService.createPendingPurchase(userAccountId, request.creditPackageId());
        return new PaymentResponse(
                payment.getId(), payment.getCreditAmount(), payment.getAmountKrw(),
                payment.getStatus().name(), payment.getCreatedAt());
    }

    /**
     * 토스 결제창에서 돌아온 뒤 호출 — orderId는 애초에 payment.id를 그대로
     * 썼으므로 경로변수만으로 어떤 결제인지 알 수 있다. 실제 검증(본인
     * 소유·PENDING 상태·금액 일치)과 토스 재확인은 CreditService에서 한다.
     */
    @PostMapping("/purchases/{paymentId}/confirm")
    public PaymentResponse confirmPurchase(
            @AuthenticationPrincipal UUID userAccountId,
            @PathVariable UUID paymentId,
            @RequestBody ConfirmPurchaseRequest request) {
        Payment completed = creditService.confirmTossPurchase(
                userAccountId, paymentId, request.paymentKey(), request.amount());
        return new PaymentResponse(
                completed.getId(), completed.getCreditAmount(), completed.getAmountKrw(),
                completed.getStatus().name(), completed.getCreatedAt());
    }

    public record CreatePurchaseRequest(UUID creditPackageId) {
    }

    public record ConfirmPurchaseRequest(String paymentKey, int amount) {
    }

    public record PackageResponse(UUID id, String name, int creditAmount, int priceKrw) {
    }

    public record BalanceResponse(int creditBalance) {
    }

    public record TransactionResponse(
            UUID id, String type, int amount, int balanceAfter, String note, Instant createdAt) {
    }

    public record PaymentResponse(
            UUID id, int creditAmount, int amountKrw, String status, Instant createdAt) {
    }
}
