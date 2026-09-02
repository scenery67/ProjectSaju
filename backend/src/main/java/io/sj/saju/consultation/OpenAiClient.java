package io.sj.saju.consultation;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper over OpenAI's chat completions endpoint. Takes a
 * {@link RestClient.Builder} (a Spring-managed bean) rather than calling
 * {@code RestClient.builder()} directly, so tests can bind
 * {@code MockRestServiceServer} to the same builder instance before this
 * component builds its client.
 */
@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final String model;
    private final boolean configured;

    public OpenAiClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-4o-mini}") String model,
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.configured = !apiKey.isBlank();
        this.model = model;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /** false when OPENAI_API_KEY isn't set — callers should fail fast rather than hit the API with an empty key. */
    public boolean isConfigured() {
        return configured;
    }

    public String chat(List<ChatMessage> messages) {
        if (!configured) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
        ChatCompletionResponse response = restClient.post()
                .uri("/chat/completions")
                .body(new ChatCompletionRequest(model, messages))
                .retrieve()
                .body(ChatCompletionResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("empty response from OpenAI");
        }
        return response.choices().get(0).message().content();
    }

    public record ChatMessage(String role, String content) {
    }

    record ChatCompletionRequest(String model, List<ChatMessage> messages) {
    }

    record ChatCompletionResponse(List<Choice> choices) {
    }

    record Choice(ChatMessage message) {
    }
}
