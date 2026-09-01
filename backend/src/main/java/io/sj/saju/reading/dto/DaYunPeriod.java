package io.sj.saju.reading.dto;

/** One 대운(大運) period: inclusive age range and its 간지 pillar (Hangul). */
public record DaYunPeriod(int startAge, int endAge, String pillar) {
}
