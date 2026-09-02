package io.sj.saju.consultation;

import io.sj.saju.persona.PersonaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One continuous LLM consultation thread, grounded in a single saju reading. */
@Entity
@Table(name = "consultation_session")
public class ConsultationSession {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_account_id", nullable = false)
    private UUID userAccountId;

    private UUID readingRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PersonaType personaType;

    @Column(nullable = false)
    private Instant createdAt;

    protected ConsultationSession() {
        // JPA
    }

    public ConsultationSession(UUID userAccountId, UUID readingRecordId, PersonaType personaType) {
        this.userAccountId = userAccountId;
        this.readingRecordId = readingRecordId;
        this.personaType = personaType;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public UUID getReadingRecordId() {
        return readingRecordId;
    }

    public PersonaType getPersonaType() {
        return personaType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
