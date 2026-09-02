package io.sj.saju.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sj.saju.auth.OAuthProvider;
import io.sj.saju.auth.UserAccount;
import io.sj.saju.auth.UserAccountRepository;
import io.sj.saju.billing.CreditService;
import io.sj.saju.billing.InsufficientCreditException;
import io.sj.saju.persona.PersonaType;
import io.sj.saju.reading.ReadingRecord;
import io.sj.saju.reading.ReadingRecordRepository;
import io.sj.saju.reading.dto.PersonalityProfile;
import io.sj.saju.reading.dto.SajuChart;
import io.sj.saju.reading.dto.SajuReadingResult;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * @SpringBootTest against the real local Postgres (like CreditServiceTest) so
 * the credit-consume/refund interaction with the real CreditService is
 * genuinely exercised. The real OpenAiClient bean is swapped for a fake via
 * @TestConfiguration — no network call, no API key needed.
 */
@SpringBootTest
@Transactional
class ConsultationServiceTest {

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ReadingRecordRepository readingRecordRepository;

    @Autowired
    private CreditService creditService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeOpenAiClient fakeOpenAiClient;

    private UserAccount user;
    private UUID readingRecordId;

    @BeforeEach
    void setUp() {
        user = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "consult-test-" + UUID.randomUUID(), "테스터"));

        SajuChart chart = new SajuChart(
                "갑자", "병인", "정묘", null, "정",
                Map.of("목", 2, "화", 4, "토", 1, "금", 0, "수", 1),
                "화", "비견", "비견", null,
                List.of(), List.of(), List.of(), List.of(),
                "장생", "장생", "장생", null,
                List.of(), List.of(),
                new PersonalityProfile("성격설명", "연애설명", "직업설명", "재물설명", "대인관계설명"));
        ReadingRecord record = readingRecordRepository.saveAndFlush(
                new ReadingRecord(PersonaType.BREAKUP, "테스터", null, "요약", "상세", user.getId(), null));
        readingRecordId = record.getId();

        SajuReadingResult result = new SajuReadingResult(record.getId(), PersonaType.BREAKUP, "요약", "상세", chart, null);
        record.setResultJson(objectMapper.writeValueAsString(result));
        readingRecordRepository.saveAndFlush(record);

        fakeOpenAiClient.reset();
    }

    @Test
    void createSessionSucceedsForAnOwnedReadingWithSavedResult() {
        ConsultationSession session = consultationService.createSession(user.getId(), readingRecordId);

        assertThat(session.getUserAccountId()).isEqualTo(user.getId());
        assertThat(session.getPersonaType()).isEqualTo(PersonaType.BREAKUP);
    }

    @Test
    void createSessionFailsForAReadingOwnedBySomeoneElse() {
        UserAccount other = userAccountRepository.saveAndFlush(
                new UserAccount(OAuthProvider.KAKAO, "other-" + UUID.randomUUID(), "다른사람"));

        assertThatThrownBy(() -> consultationService.createSession(other.getId(), readingRecordId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void sendMessageConsumesOneCreditAndStoresBothSides() {
        creditService.grantFree(user.getId(), 5, null, "테스트 시드");
        ConsultationSession session = consultationService.createSession(user.getId(), readingRecordId);
        fakeOpenAiClient.nextReply = "괜찮아질 거예요.";

        ConsultationMessage reply = consultationService.sendMessage(user.getId(), session.getId(), "저 괜찮을까요?");

        assertThat(reply.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(reply.getContent()).isEqualTo("괜찮아질 거예요.");
        assertThat(balanceOf(user.getId())).isEqualTo(4);

        List<ConsultationMessage> messages = consultationService.messages(user.getId(), session.getId());
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
    }

    @Test
    void sendMessageRefundsTheCreditWhenTheLlmCallFails() {
        creditService.grantFree(user.getId(), 5, null, "테스트 시드");
        ConsultationSession session = consultationService.createSession(user.getId(), readingRecordId);
        fakeOpenAiClient.shouldFail = true;

        assertThatThrownBy(() -> consultationService.sendMessage(user.getId(), session.getId(), "질문"))
                .isInstanceOf(ConsultationFailedException.class);

        assertThat(balanceOf(user.getId())).isEqualTo(5);
        // 실패했어도 사용자가 실제로 보낸 질문 자체는 기록에 남아있어야 한다.
        assertThat(consultationService.messages(user.getId(), session.getId())).hasSize(1);
    }

    @Test
    void sendMessageFailsWithoutEnoughCredit() {
        ConsultationSession session = consultationService.createSession(user.getId(), readingRecordId);

        assertThatThrownBy(() -> consultationService.sendMessage(user.getId(), session.getId(), "질문"))
                .isInstanceOf(InsufficientCreditException.class);
    }

    private int balanceOf(UUID userAccountId) {
        return userAccountRepository.findById(userAccountId).orElseThrow().getCreditBalance();
    }

    @TestConfiguration
    static class FakeOpenAiClientConfig {
        @Bean
        @Primary
        FakeOpenAiClient fakeOpenAiClient() {
            return new FakeOpenAiClient(RestClient.builder(), "", "gpt-4o-mini", "http://localhost");
        }
    }

    static class FakeOpenAiClient extends OpenAiClient {
        String nextReply = "기본 응답";
        boolean shouldFail = false;

        FakeOpenAiClient(RestClient.Builder builder, String apiKey, String model, String baseUrl) {
            super(builder, apiKey, model, baseUrl);
        }

        void reset() {
            shouldFail = false;
            nextReply = "기본 응답";
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public String chat(List<ChatMessage> messages) {
            if (shouldFail) {
                throw new RuntimeException("simulated upstream failure");
            }
            return nextReply;
        }
    }
}
