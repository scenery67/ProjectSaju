package io.sj.saju.reading.dto;

import io.sj.saju.persona.PersonaType;

/**
 * partnerChart is null for the single-person (breakup) reading.
 */
public record SajuReadingResult(
        PersonaType personaType,
        String summary,
        String detail,
        SajuChart selfChart,
        SajuChart partnerChart) {
}
