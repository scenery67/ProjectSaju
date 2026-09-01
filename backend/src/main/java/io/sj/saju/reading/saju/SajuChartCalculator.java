package io.sj.saju.reading.saju;

import com.nlf.calendar.EightChar;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import io.sj.saju.reading.CalendarType;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.SajuChart;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns birth info into a 사주팔자 chart using the cn.6tail:lunar library, which
 * derives year/day pillars from the exact 절기(solar term) boundaries rather
 * than the lunar new year — required for a correct 만세력 계산.
 * NOTE: solar term instants in the library follow the traditional East-Asian
 * (China-origin) almanac; they are not re-derived for Korea's longitude, so a
 * birth time within ~a few hours of a 절기 change can land on the wrong side.
 * Acceptable for now, worth revisiting before treating results as authoritative.
 */
public final class SajuChartCalculator {

    private static final String GAN_HANJA = "甲乙丙丁戊己庚辛壬癸";
    private static final String GAN_HANGUL = "갑을병정무기경신임계";
    private static final String ZHI_HANJA = "子丑寅卯辰巳午未申酉戌亥";
    private static final String ZHI_HANGUL = "자축인묘진사오미신유술해";
    private static final String WUXING_HANJA = "木火土金水";
    private static final String WUXING_HANGUL = "목화토금수";

    private SajuChartCalculator() {
    }

    public static SajuChart calculate(PersonInput input) {
        boolean hasBirthTime = input.birthTime() != null;
        LocalTime time = hasBirthTime ? LocalTime.parse(input.birthTime()) : LocalTime.MIDNIGHT;

        EightChar eightChar = toLunar(input, time).getEightChar();

        Map<String, Integer> fiveElementCounts = new LinkedHashMap<>();
        for (char c : WUXING_HANGUL.toCharArray()) {
            fiveElementCounts.put(String.valueOf(c), 0);
        }
        tallyWuxing(fiveElementCounts, eightChar.getYearWuXing());
        tallyWuxing(fiveElementCounts, eightChar.getMonthWuXing());
        tallyWuxing(fiveElementCounts, eightChar.getDayWuXing());
        if (hasBirthTime) {
            tallyWuxing(fiveElementCounts, eightChar.getTimeWuXing());
        }

        String dominant = fiveElementCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new SajuChart(
                toHangulPillar(eightChar.getYear()),
                toHangulPillar(eightChar.getMonth()),
                toHangulPillar(eightChar.getDay()),
                hasBirthTime ? toHangulPillar(eightChar.getTime()) : null,
                toHangul(eightChar.getDayGan().charAt(0), GAN_HANJA, GAN_HANGUL),
                fiveElementCounts,
                dominant);
    }

    private static Lunar toLunar(PersonInput input, LocalTime time) {
        LocalDate date = input.birthDate();
        if (input.calendarType() == CalendarType.SOLAR) {
            return Solar.fromYmdHms(
                            date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                            time.getHour(), time.getMinute(), 0)
                    .getLunar();
        }
        // lunar-java convention: leap month is encoded as a negative month value.
        int month = input.isLunarLeapMonth() ? -date.getMonthValue() : date.getMonthValue();
        return Lunar.fromYmdHms(
                date.getYear(), month, date.getDayOfMonth(),
                time.getHour(), time.getMinute(), 0);
    }

    private static void tallyWuxing(Map<String, Integer> counts, String wuxingHanja) {
        for (char c : wuxingHanja.toCharArray()) {
            counts.merge(toHangul(c, WUXING_HANJA, WUXING_HANGUL), 1, Integer::sum);
        }
    }

    private static String toHangulPillar(String hanjaPillar) {
        return toHangul(hanjaPillar.charAt(0), GAN_HANJA, GAN_HANGUL)
                + toHangul(hanjaPillar.charAt(1), ZHI_HANJA, ZHI_HANGUL);
    }

    private static String toHangul(char hanja, String hanjaTable, String hangulTable) {
        int idx = hanjaTable.indexOf(hanja);
        return idx >= 0 ? String.valueOf(hangulTable.charAt(idx)) : String.valueOf(hanja);
    }
}
