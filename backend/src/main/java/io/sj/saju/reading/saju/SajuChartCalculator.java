package io.sj.saju.reading.saju;

import com.nlf.calendar.EightChar;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import com.nlf.calendar.eightchar.DaYun;
import com.nlf.calendar.util.LunarUtil;
import io.sj.saju.reading.CalendarType;
import io.sj.saju.reading.Gender;
import io.sj.saju.reading.dto.DaYunPeriod;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.SajuChart;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    // 십성(十神) 10종. cn.6tail:lunar의 LunarUtil.SHI_SHEN 값(한자)을 그대로 키로 쓰고
    // 화면 표시용 한글만 우리가 매핑한다 — 십성 판정 로직 자체는 라이브러리에 위임.
    private static final String[] TEN_GOD_HANJA = {
        "比肩", "劫财", "食神", "伤官", "偏财", "正财", "七杀", "正官", "偏印", "正印"
    };
    private static final String[] TEN_GOD_HANGUL = {
        "비견", "겁재", "식신", "상관", "편재", "정재", "편관", "정관", "편인", "정인"
    };

    // 표시할 대운 개수 (10년 단위 8개 = 80세까지).
    private static final int DA_YUN_COUNT = 8;

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
                dominant,
                toHangulTenGod(eightChar.getYearShiShenGan()),
                toHangulTenGod(eightChar.getMonthShiShenGan()),
                hasBirthTime ? toHangulTenGod(eightChar.getTimeShiShenGan()) : null,
                daYunPeriods(eightChar, input.gender()));
    }

    /**
     * 십성(十神): 상대방의 일간(dayMaster)이 나에게 어떤 관계인지 조회할 때도 쓴다
     * (궁합에서 "상대는 나에게 OO에 해당" 같은 문구용). 두 값 모두 이 클래스의
     * toHangul 표기(한글 1글자)를 입력으로 받는다.
     */
    public static String tenGodOfGan(String dayMasterHangul, String otherGanHangul) {
        char dayMasterHanja = toHanja(dayMasterHangul.charAt(0), GAN_HANGUL, GAN_HANJA);
        char otherHanja = toHanja(otherGanHangul.charAt(0), GAN_HANGUL, GAN_HANJA);
        String hanja = LunarUtil.SHI_SHEN.get("" + dayMasterHanja + otherHanja);
        return hanja == null ? null : toHangulTenGod(hanja);
    }

    private static List<DaYunPeriod> daYunPeriods(EightChar eightChar, Gender gender) {
        int genderCode = gender == Gender.MALE ? 1 : 0;
        DaYun[] daYuns = eightChar.getYun(genderCode).getDaYun(DA_YUN_COUNT);
        List<DaYunPeriod> periods = new ArrayList<>(daYuns.length);
        for (DaYun daYun : daYuns) {
            String ganZhi = daYun.getGanZhi();
            if (ganZhi == null || ganZhi.isEmpty()) {
                continue; // 첫 항목은 대운 시작 전 구간이라 간지가 비어 있을 수 있다.
            }
            periods.add(new DaYunPeriod(daYun.getStartAge(), daYun.getEndAge(), toHangulPillar(ganZhi)));
        }
        return periods;
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

    private static char toHanja(char hangul, String hangulTable, String hanjaTable) {
        int idx = hangulTable.indexOf(hangul);
        return idx >= 0 ? hanjaTable.charAt(idx) : hangul;
    }

    private static String toHangulTenGod(String hanjaTenGod) {
        for (int i = 0; i < TEN_GOD_HANJA.length; i++) {
            if (TEN_GOD_HANJA[i].equals(hanjaTenGod)) {
                return TEN_GOD_HANGUL[i];
            }
        }
        return hanjaTenGod;
    }
}
