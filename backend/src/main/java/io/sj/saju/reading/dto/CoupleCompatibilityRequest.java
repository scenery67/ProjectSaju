package io.sj.saju.reading.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CoupleCompatibilityRequest(
        @NotNull @Valid PersonInput self,
        @NotNull @Valid PersonInput partner) {
}
