package io.sj.saju.consultation;

import io.sj.saju.billing.CreditService;
import io.sj.saju.reading.ReadingRecord;
import io.sj.saju.reading.ReadingRecordRepository;
import io.sj.saju.reading.dto.SajuReadingResult;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Orchestrates one LLM consultation turn: consume a credit, call the LLM with
 * the saju context plus prior turns, persist both sides. A session is always
 * grounded in one reading_record (its result_json — see SajuReadingService)
 * so the LLM always answers from the same computed chart the user saw.
 */
@Service
public class ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);
    private static final int CREDIT_COST_PER_MESSAGE = 1;

    private final ConsultationSessionRepository sessionRepository;
    private final ConsultationMessageRepository messageRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final CreditService creditService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public ConsultationService(
            ConsultationSessionRepository sessionRepository,
            ConsultationMessageRepository messageRepository,
            ReadingRecordRepository readingRecordRepository,
            CreditService creditService,
            OpenAiClient openAiClient,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.readingRecordRepository = readingRecordRepository;
        this.creditService = creditService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    /** readingRecordId must belong to this user and have a saved result (see reading_record.result_json). */
    @Transactional
    public ConsultationSession createSession(UUID userAccountId, UUID readingRecordId) {
        ReadingRecord record = readingRecordRepository.findById(readingRecordId)
                .filter(r -> userAccountId.equals(r.getUserAccountId()))
                .orElseThrow(() -> new NoSuchElementException("reading record not found: " + readingRecordId));
        if (record.getResultJson() == null) {
            throw new IllegalStateException("reading record has no saved result: " + readingRecordId);
        }
        return sessionRepository.save(
                new ConsultationSession(userAccountId, readingRecordId, record.getPersonaType()));
    }

    public List<ConsultationSession> sessions(UUID userAccountId) {
        return sessionRepository.findByUserAccountIdOrderByCreatedAtDesc(userAccountId);
    }

    public List<ConsultationMessage> messages(UUID userAccountId, UUID sessionId) {
        ConsultationSession session = requireOwnedSession(userAccountId, sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
    }

    /**
     * 질문 1건 = 크레딧 1개. LLM 호출 전에 먼저 차감해서(CreditService의
     * 원자적 SQL) 동시 요청으로 인한 이중 소비를 막는다. LLM 호출이 실패하면
     * (키 미설정, API 오류, 타임아웃 등) 답을 못 준 것이므로 차감한 크레딧을
     * grantFree로 즉시 되돌려준다 — 대가 없이 크레딧만 잃는 상황을 막는다.
     */
    @Transactional
    public ConsultationMessage sendMessage(UUID userAccountId, UUID sessionId, String userContent) {
        ConsultationSession session = requireOwnedSession(userAccountId, sessionId);
        SajuReadingResult context = loadContext(session);

        creditService.consume(userAccountId, CREDIT_COST_PER_MESSAGE, session.getId(), "상담 질문");
        messageRepository.save(new ConsultationMessage(session.getId(), MessageRole.USER, userContent));

        List<OpenAiClient.ChatMessage> history = buildChatHistory(session, context);
        String assistantContent;
        try {
            assistantContent = openAiClient.chat(history);
        } catch (RuntimeException e) {
            log.warn("LLM call failed for session {}, refunding the consumed credit", sessionId, e);
            creditService.grantFree(userAccountId, CREDIT_COST_PER_MESSAGE, session.getId(), "상담 응답 실패 자동 환급");
            throw new ConsultationFailedException("LLM 응답을 받아오지 못했어요", e);
        }

        return messageRepository.save(
                new ConsultationMessage(session.getId(), MessageRole.ASSISTANT, assistantContent));
    }

    private List<OpenAiClient.ChatMessage> buildChatHistory(ConsultationSession session, SajuReadingResult context) {
        List<OpenAiClient.ChatMessage> history = new ArrayList<>();
        history.add(new OpenAiClient.ChatMessage("system", ConsultationPromptBuilder.systemPrompt(context)));
        for (ConsultationMessage m : messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())) {
            history.add(new OpenAiClient.ChatMessage(m.getRole().name().toLowerCase(), m.getContent()));
        }
        return history;
    }

    private SajuReadingResult loadContext(ConsultationSession session) {
        if (session.getReadingRecordId() == null) {
            throw new IllegalStateException("session has no linked reading record: " + session.getId());
        }
        ReadingRecord record = readingRecordRepository.findById(session.getReadingRecordId())
                .orElseThrow(() -> new NoSuchElementException("reading record not found: " + session.getReadingRecordId()));
        try {
            return objectMapper.readValue(record.getResultJson(), SajuReadingResult.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("failed to load saju context for consultation " + session.getId(), e);
        }
    }

    private ConsultationSession requireOwnedSession(UUID userAccountId, UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> userAccountId.equals(s.getUserAccountId()))
                .orElseThrow(() -> new NoSuchElementException("session not found: " + sessionId));
    }
}
