package io.sj.saju.billing;

import io.sj.saju.auth.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 사용자 본인의 크레딧/결제 화면용 API. 마이페이지의
 * "보유 크레딧/충전하기/결제내역"에 대응한다. PG 연동 전이라 purchases는
 * PENDING 결제 레코드만 만들고, 실제 승인/완료는 아직 붙지 않았다.
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

    public record CreatePurchaseRequest(UUID creditPackageId) {
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
