package io.sj.saju.reading.saju;

import static org.assertj.core.api.Assertions.assertThat;

import io.sj.saju.reading.CalendarType;
import io.sj.saju.reading.Gender;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.SajuChart;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SajuChartCalculatorTest {

    private static final String GAN = "갑을병정무기경신임계";
    private static final String ZHI = "자축인묘진사오미신유술해";

    @Test
    void solarInputWithBirthTimeProducesFourPillars() {
        PersonInput input = new PersonInput(
                "테스트", LocalDate.of(1990, 5, 20), "14:30", CalendarType.SOLAR, false, Gender.FEMALE);

        SajuChart chart = SajuChartCalculator.calculate(input);

        assertPillar(chart.yearPillar());
        assertPillar(chart.monthPillar());
        assertPillar(chart.dayPillar());
        assertPillar(chart.hourPillar());
        assertThat(GAN).contains(chart.dayMaster());
        assertThat(chart.fiveElementCounts().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(8);
        assertThat(chart.dominantFiveElement()).isIn("목", "화", "토", "금", "수");
    }

    @Test
    void unknownBirthTimeOmitsHourPillar() {
        PersonInput input = new PersonInput(
                "테스트", LocalDate.of(1990, 5, 20), null, CalendarType.SOLAR, false, Gender.MALE);

        SajuChart chart = SajuChartCalculator.calculate(input);

        assertThat(chart.hourPillar()).isNull();
        assertThat(chart.fiveElementCounts().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(6);
    }

    @Test
    void lunarLeapMonthInputDoesNotThrow() {
        // 2020 lunar calendar has a leap 4th month (윤4월, ~2020-05-23 to 2020-06-20 solar).
        PersonInput input = new PersonInput(
                "테스트", LocalDate.of(2020, 4, 15), "09:00", CalendarType.LUNAR, true, Gender.MALE);

        SajuChart chart = SajuChartCalculator.calculate(input);

        assertPillar(chart.yearPillar());
    }

    @Test
    void yearPillarChangesAtSolarTermBoundaryNotLunarNewYear() {
        // 실제(한국 기준) 입춘(立春)은 2024-02-04 17:27 KST다.
        // 출처: 국제뉴스 2024년 입춘시간 보도(https://www.gukjenews.com/news/articleView.html?idxno=2914181).
        // 그런데 cn.6tail:lunar는 한국 경도로 절기를 보정하지 않아, 실측 결과 이 라이브러리는
        // 16:27→16:28 사이에서 년주를 전환한다 — 실제보다 약 59분 이르다. 이는 README/CLAUDE.md에
        // 적어둔 "절기 시각 미보정" 한계를 구체적인 시각 차이로 고정해두는 테스트다.
        // (라이브러리가 갱신되어 이 경계가 달라지면 이 테스트가 먼저 깨져서 알려준다.)
        PersonInput justBefore = new PersonInput(
                "테스트", LocalDate.of(2024, 2, 4), "16:27", CalendarType.SOLAR, false, Gender.MALE);
        PersonInput justAfter = new PersonInput(
                "테스트", LocalDate.of(2024, 2, 4), "16:28", CalendarType.SOLAR, false, Gender.MALE);

        SajuChart before = SajuChartCalculator.calculate(justBefore);
        SajuChart after = SajuChartCalculator.calculate(justAfter);

        assertThat(before.yearPillar()).isEqualTo("계묘"); // 2023 계묘년 (라이브러리 기준, 실제 입춘 전)
        assertThat(after.yearPillar()).isEqualTo("갑진"); // 2024 갑진년으로 전환
    }

    @Test
    void lateZiHourKeepsCurrentDayPillarUnderDefaultSect() {
        // cn.6tail:lunar의 EightChar 기본 유파(sect=2)는 야자시(23:00~24:00)의 일주를
        // "다음날"이 아니라 "당일"로 계산한다. SajuChartCalculator는 sect를 별도로 지정하지
        // 않으므로 이 기본값을 그대로 쓴다 — 규칙을 바꾸려면 이 테스트부터 고쳐야 한다.
        PersonInput noon = new PersonInput(
                "테스트", LocalDate.of(2024, 3, 10), "12:00", CalendarType.SOLAR, false, Gender.MALE);
        PersonInput lateZi = new PersonInput(
                "테스트", LocalDate.of(2024, 3, 10), "23:30", CalendarType.SOLAR, false, Gender.MALE);
        PersonInput nextDayEarlyZi = new PersonInput(
                "테스트", LocalDate.of(2024, 3, 11), "00:30", CalendarType.SOLAR, false, Gender.MALE);

        SajuChart noonChart = SajuChartCalculator.calculate(noon);
        SajuChart lateZiChart = SajuChartCalculator.calculate(lateZi);
        SajuChart nextDayChart = SajuChartCalculator.calculate(nextDayEarlyZi);

        // 23:30 출생도 같은 날 12:00과 같은 일주를 받는다 (sect=2: 야자시는 당일).
        assertThat(lateZiChart.dayPillar()).isEqualTo(noonChart.dayPillar());
        // 다음날 00:30은 실제로 하루가 지났으므로 일주가 60갑자 순환으로 하루만큼 진행된다.
        assertThat(nextDayChart.dayPillar()).isEqualTo(nextDayInSixtyCycle(lateZiChart.dayPillar()));
    }

    @Test
    void leapDayFebruary29DoesNotThrow() {
        PersonInput input = new PersonInput(
                "테스트", LocalDate.of(2024, 2, 29), "10:00", CalendarType.SOLAR, false, Gender.FEMALE);

        SajuChart chart = SajuChartCalculator.calculate(input);

        assertPillar(chart.dayPillar());
    }

    private static String nextDayInSixtyCycle(String pillar) {
        int ganIndex = (GAN.indexOf(pillar.charAt(0)) + 1) % GAN.length();
        int zhiIndex = (ZHI.indexOf(pillar.charAt(1)) + 1) % ZHI.length();
        return "" + GAN.charAt(ganIndex) + ZHI.charAt(zhiIndex);
    }

    private static void assertPillar(String pillar) {
        assertThat(pillar).hasSize(2);
        assertThat(GAN).contains(String.valueOf(pillar.charAt(0)));
        assertThat(ZHI).contains(String.valueOf(pillar.charAt(1)));
    }
}
