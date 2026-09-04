package io.sj.saju.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** OpenAiClientTest와 같은 패턴 — 실제 토스 서버 없이 가짜 HTTP 서버로 요청/응답 형태만 검증한다. */
class TossPaymentsClientTest {

    private static final String EXPECTED_AUTH_HEADER =
            "Basic " + Base64.getEncoder().encodeToString("test_sk_dummy:".getBytes(StandardCharsets.UTF_8));

    @Test
    void confirmPaymentSendsBasicAuthAndOrderDetailsAndParsesTheResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(builder, "test_sk_dummy", "https://api.tosspayments.com");

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", EXPECTED_AUTH_HEADER))
                .andExpect(jsonPath("$.paymentKey").value("pay_123"))
                .andExpect(jsonPath("$.orderId").value("order_456"))
                .andExpect(jsonPath("$.amount").value(2900))
                .andRespond(withSuccess(
                        """
                        {"paymentKey":"pay_123","orderId":"order_456","status":"DONE","totalAmount":2900}
                        """,
                        MediaType.APPLICATION_JSON));

        TossPaymentsClient.ConfirmedPayment result = client.confirmPayment("pay_123", "order_456", 2900);

        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.totalAmount()).isEqualTo(2900);
        server.verify();
    }

    @Test
    void isConfiguredIsFalseWhenSecretKeyIsBlank() {
        TossPaymentsClient client = new TossPaymentsClient(RestClient.builder(), "", "https://api.tosspayments.com");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.confirmPayment("pay_123", "order_456", 2900))
                .isInstanceOf(TossPaymentFailedException.class);
    }

    @Test
    void confirmPaymentThrowsWhenTossRejectsIt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsClient client = new TossPaymentsClient(builder, "test_sk_dummy", "https://api.tosspayments.com");

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":\"REJECT_CARD_COMPANY\",\"message\":\"거절\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.confirmPayment("pay_123", "order_456", 2900))
                .isInstanceOf(TossPaymentFailedException.class)
                .hasMessageContaining("REJECT_CARD_COMPANY");
    }
}
