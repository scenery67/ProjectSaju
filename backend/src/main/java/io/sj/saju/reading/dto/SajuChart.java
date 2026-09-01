package io.sj.saju.reading.dto;

import java.util.List;
import java.util.Map;

/**
 * One person's computed 사주팔자 (EightChar) chart, already translated to Hangul
 * for display. hourPillar/timeTenGod are null when the person's birth time is
 * unknown. yearTenGod/monthTenGod/timeTenGod are the ten-gods (십성) of each
 * pillar's gan relative to this person's own day master (dayMaster) — the day
 * pillar itself has none (일간 자신은 십성 기준점이라 값이 없다).
 */
public record SajuChart(
        String yearPillar,
        String monthPillar,
        String dayPillar,
        String hourPillar,
        String dayMaster,
        Map<String, Integer> fiveElementCounts,
        String dominantFiveElement,
        String yearTenGod,
        String monthTenGod,
        String timeTenGod,
        List<DaYunPeriod> daYunPeriods) {
}
