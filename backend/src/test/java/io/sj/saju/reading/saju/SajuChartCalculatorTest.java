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

    private static void assertPillar(String pillar) {
        assertThat(pillar).hasSize(2);
        assertThat(GAN).contains(String.valueOf(pillar.charAt(0)));
        assertThat(ZHI).contains(String.valueOf(pillar.charAt(1)));
    }
}
