package io.sj.saju.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Plain unit test — no Spring context, no real API key needed. Verifies the
 * request OpenAiClient sends and the response it parses, against a faked
 * HTTP server rather than the real OpenAI endpoint.
 */
class OpenAiClientTest {

    @Test
    void chatSendsModelAndMessagesAndParsesTheReply() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiClient client = new OpenAiClient(builder, "test-key", "gpt-4o-mini", "https://api.openai.com/v1");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"message":{"role":"assistant","content":"괜찮아질 거예요."}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        String reply = client.chat(List.of(
                new OpenAiClient.ChatMessage("system", "당신은 다정한 상담사입니다."),
                new OpenAiClient.ChatMessage("user", "오늘 기분이 어때요?")));

        assertThat(reply).isEqualTo("괜찮아질 거예요.");
        server.verify();
    }

    @Test
    void isConfiguredIsFalseWhenApiKeyIsBlank() {
        OpenAiClient client = new OpenAiClient(RestClient.builder(), "", "gpt-4o-mini", "https://api.openai.com/v1");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.chat(List.of(new OpenAiClient.ChatMessage("user", "hi"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void chatThrowsWhenTheUpstreamCallFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiClient client = new OpenAiClient(builder, "test-key", "gpt-4o-mini", "https://api.openai.com/v1");

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.chat(List.of(new OpenAiClient.ChatMessage("user", "hi"))))
                .isInstanceOf(RuntimeException.class);
    }
}
