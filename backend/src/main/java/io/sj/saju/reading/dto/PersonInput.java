package io.sj.saju.reading.dto;

import io.sj.saju.reading.CalendarType;
import io.sj.saju.reading.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Birth info for one person. All external input is validated here at the
 * controller boundary — never trust client-supplied dates/strings downstream.
 * 외부 입력은 항상 이 지점(컨트롤러 경계)에서 검증한다.
 */
public record PersonInput(
        @NotBlank @Size(max = 20) String name,
        @NotNull @Past LocalDate birthDate,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "birthTime must be HH:mm") String birthTime,
        @NotNull CalendarType calendarType,
        boolean isLunarLeapMonth,
        @NotNull Gender gender) {
}
