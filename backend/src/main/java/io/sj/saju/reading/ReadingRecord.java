package io.sj.saju.reading;

import io.sj.saju.persona.PersonaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persisted history of a single reading request/result pair. */
@Entity
@Table(name = "reading_record")
public class ReadingRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PersonaType personaType;

    @Column(nullable = false)
    private String selfName;

    private String partnerName;

    @Column(nullable = false)
    private String summary;

    @Lob
    @Column(nullable = false)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt;

    protected ReadingRecord() {
        // JPA
    }

    public ReadingRecord(PersonaType personaType, String selfName, String partnerName,
            String summary, String detail) {
        this.personaType = personaType;
        this.selfName = selfName;
        this.partnerName = partnerName;
        this.summary = summary;
        this.detail = detail;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public PersonaType getPersonaType() {
        return personaType;
    }

    public String getSelfName() {
        return selfName;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
