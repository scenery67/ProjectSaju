package io.sj.saju.reading;

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

    // Plain Postgres `text` column, not @Lob — @Lob maps String to `oid`
    // (large object) on Postgres, which needs special handling and doesn't
    // dump/query like normal text. `text` has no length cap either way.
    @Column(nullable = false, columnDefinition = "text")
    private String detail;

    @Column(nullable = false)
    private Instant createdAt;

    // 로그인한 사용자의 기록만 채워진다 — 비로그인 요청은 계속 null(CLAUDE.md
    // 3.2, 보관 정책 결정 전까지 식별자를 넣지 않는다는 원칙). 계정 삭제 시
    // ON DELETE CASCADE로 이 기록도 함께 삭제된다(V5 마이그레이션).
    private UUID userAccountId;

    // "내 사주"에서 결과를 다시 열어볼 수 있게 전체 결과(SajuReadingResult)를
    // JSON으로 저장한다 — userAccountId와 마찬가지로 로그인 기록에만 채워진다.
    @Column(columnDefinition = "text")
    private String resultJson;

    protected ReadingRecord() {
        // JPA
    }

    public ReadingRecord(PersonaType personaType, String selfName, String partnerName,
            String summary, String detail, UUID userAccountId, String resultJson) {
        this.personaType = personaType;
        this.selfName = selfName;
        this.partnerName = partnerName;
        this.summary = summary;
        this.detail = detail;
        this.createdAt = Instant.now();
        this.userAccountId = userAccountId;
        this.resultJson = resultJson;
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

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public String getResultJson() {
        return resultJson;
    }
}
