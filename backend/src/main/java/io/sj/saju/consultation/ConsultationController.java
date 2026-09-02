package io.sj.saju.consultation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

// /api/consultation/**은 permitAll 목록에 없어 SecurityConfig의
// anyRequest().authenticated()가 그대로 적용된다 — 로그인 필요.
@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(
            @AuthenticationPrincipal UUID userAccountId, @RequestBody CreateSessionRequest request) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        ConsultationSession session = consultationService.createSession(userAccountId, request.readingRecordId());
        return ResponseEntity.ok(toResponse(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> sessions(@AuthenticationPrincipal UUID userAccountId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(
                consultationService.sessions(userAccountId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<MessageResponse>> messages(
            @AuthenticationPrincipal UUID userAccountId, @PathVariable UUID sessionId) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(
                consultationService.messages(userAccountId, sessionId).stream().map(this::toResponse).toList());
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UUID userAccountId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        if (userAccountId == null) {
            return ResponseEntity.status(401).build();
        }
        ConsultationMessage reply = consultationService.sendMessage(userAccountId, sessionId, request.content());
        return ResponseEntity.ok(toResponse(reply));
    }

    private SessionResponse toResponse(ConsultationSession s) {
        return new SessionResponse(s.getId(), s.getPersonaType().name(), s.getReadingRecordId(), s.getCreatedAt());
    }

    private MessageResponse toResponse(ConsultationMessage m) {
        return new MessageResponse(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt());
    }

    public record CreateSessionRequest(UUID readingRecordId) {
    }

    public record SendMessageRequest(@NotBlank @Size(max = 2000) String content) {
    }

    public record SessionResponse(UUID id, String personaType, UUID readingRecordId, Instant createdAt) {
    }

    public record MessageResponse(UUID id, String role, String content, Instant createdAt) {
    }
}
