package io.sj.saju.billing;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 토스페이먼츠 결제 승인 API를 호출하는 얇은 래퍼. 프론트가 결제창을 띄우고
 * 받은 paymentKey/orderId/amount를 그대로 믿지 않고, 여기서 토스 서버에
 * 직접 재확인한 뒤에만 크레딧을 지급한다(BillingController 참고) — 클라이언트
 * 값만으로 크레딧을 주면 조작된 금액으로 지급받을 수 있다.
 *
 * <p>OpenAiClient와 같은 이유로 RestClient.Builder를 주입받는다(테스트에서
 * MockRestServiceServer를 그 빌더에 바인딩할 수 있게).
 */
@Component
public class TossPaymentsClient {

    private final RestClient restClient;
    private final boolean configured;

    public TossPaymentsClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.toss.secret-key:}") String secretKey,
            @Value("${app.toss.base-url:https://api.tosspayments.com}") String baseUrl) {
        this.configured = !secretKey.isBlank();
        // 토스 API는 시크릿 키를 Basic 인증의 username으로, 비밀번호는 빈
        // 문자열로 보내는 방식을 쓴다(공식 문서 방식) — "시크릿키:" 그대로 인코딩.
        String basicAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Basic " + basicAuth)
                .build();
    }

    /** false면 TOSS_SECRET_KEY가 설정 안 된 것 — 호출하면 바로 실패시킨다. */
    public boolean isConfigured() {
        return configured;
    }

    public ConfirmedPayment confirmPayment(String paymentKey, String orderId, int amount) {
        if (!configured) {
            throw new TossPaymentFailedException("TOSS_SECRET_KEY is not configured", null);
        }
        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .body(new ConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(ConfirmedPayment.class);
        } catch (RestClientResponseException e) {
            throw new TossPaymentFailedException(
                    "토스 결제 승인 실패 (status=%d): %s".formatted(e.getStatusCode().value(), e.getResponseBodyAsString()),
                    e);
        }
    }

    record ConfirmRequest(String paymentKey, String orderId, int amount) {
    }

    /** 토스 응답 중 우리가 실제로 쓰는 필드만 매핑한다 — 나머지는 무시. */
    public record ConfirmedPayment(String paymentKey, String orderId, String status, int totalAmount) {
    }
}
