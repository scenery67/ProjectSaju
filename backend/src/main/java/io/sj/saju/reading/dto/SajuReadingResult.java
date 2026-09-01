package io.sj.saju.reading.dto;

import io.sj.saju.persona.PersonaType;

public record SajuReadingResult(PersonaType personaType, String summary, String detail) {
}
