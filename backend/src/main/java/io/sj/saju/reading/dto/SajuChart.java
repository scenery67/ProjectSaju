package io.sj.saju.reading.dto;

import java.util.Map;

/**
 * One person's computed 사주팔자 (EightChar) chart, already translated to Hangul
 * for display. hourPillar is null when the person's birth time is unknown.
 */
public record SajuChart(
        String yearPillar,
        String monthPillar,
        String dayPillar,
        String hourPillar,
        String dayMaster,
        Map<String, Integer> fiveElementCounts,
        String dominantFiveElement) {
}
