package io.sj.saju.reading.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BreakupReadingRequest(@NotNull @Valid PersonInput self) {
}
