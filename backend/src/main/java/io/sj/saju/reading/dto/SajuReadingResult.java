package io.sj.saju.reading.dto;

import io.sj.saju.persona.PersonaType;
import java.util.UUID;

/**
 * partnerChart is null for the single-person (breakup) reading. id is the
 * underlying reading_record's id — null-safe for older stored results
 * (result_json saved before this field existed) since it's a reference type.
 * Non-null id + logged in is what a client needs to start an LLM consultation
 * (POST /api/consultation/sessions) grounded in this exact reading.
 */
public record SajuReadingResult(
        UUID id,
        PersonaType personaType,
        String summary,
        String detail,
        SajuChart selfChart,
        SajuChart partnerChart) {
}
