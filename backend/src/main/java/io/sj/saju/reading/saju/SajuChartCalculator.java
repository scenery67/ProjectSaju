package io.sj.saju.reading.saju;

import com.nlf.calendar.EightChar;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import com.nlf.calendar.eightchar.DaYun;
import com.nlf.calendar.eightchar.LiuNian;
import com.nlf.calendar.util.LunarUtil;
import io.sj.saju.reading.CalendarType;
import io.sj.saju.reading.Gender;
import io.sj.saju.reading.dto.DaYunPeriod;
import io.sj.saju.reading.dto.LiuNianPeriod;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.PersonalityProfile;
import io.sj.saju.reading.dto.SajuChart;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumMap;
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

    // 12운성(長生十二神). 실제로 라이브러리가 반환하는 한자 12개를 실측해서 확정했다
    // (표준 순서: 长生=장생, 临官은 관행상 '건록'으로 표기).
    private static final String[] TWELVE_STAGE_HANJA = {
        "长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养"
    };
    private static final String[] TWELVE_STAGE_HANGUL = {
        "장생", "목욕", "관대", "건록", "제왕", "쇠", "병", "사", "묘", "절", "태", "양"
    };

    // 표시할 대운 개수 (10년 단위 8개 = 80세까지).
    private static final int DA_YUN_COUNT = 8;
    // 세운(歲運)은 현재 지나고 있는 대운 구간의 10년만 보여준다.
    private static final int LIU_NIAN_COUNT = 10;

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

        String dayMaster = toHangul(eightChar.getDayGan().charAt(0), GAN_HANJA, GAN_HANGUL);
        String yearTenGod = toHangulTenGod(eightChar.getYearShiShenGan());
        String monthTenGod = toHangulTenGod(eightChar.getMonthShiShenGan());
        String timeTenGod = hasBirthTime ? toHangulTenGod(eightChar.getTimeShiShenGan()) : null;

        int genderCode = input.gender() == Gender.MALE ? 1 : 0;
        DaYun[] daYuns = eightChar.getYun(genderCode).getDaYun(DA_YUN_COUNT);

        return new SajuChart(
                toHangulPillar(eightChar.getYear()),
                toHangulPillar(eightChar.getMonth()),
                toHangulPillar(eightChar.getDay()),
                hasBirthTime ? toHangulPillar(eightChar.getTime()) : null,
                dayMaster,
                fiveElementCounts,
                dominant,
                yearTenGod,
                monthTenGod,
                timeTenGod,
                toHangulHideGan(eightChar.getYearHideGan()),
                toHangulHideGan(eightChar.getMonthHideGan()),
                toHangulHideGan(eightChar.getDayHideGan()),
                hasBirthTime ? toHangulHideGan(eightChar.getTimeHideGan()) : null,
                toHangulTwelveStage(eightChar.getYearDiShi()),
                toHangulTwelveStage(eightChar.getMonthDiShi()),
                toHangulTwelveStage(eightChar.getDayDiShi()),
                hasBirthTime ? toHangulTwelveStage(eightChar.getTimeDiShi()) : null,
                toDaYunPeriods(daYuns),
                currentLiuNian(daYuns, dayMaster, input.birthDate()),
                buildPersonalityProfile(dayMaster, yearTenGod, monthTenGod, timeTenGod));
    }

    /**
     * 성격은 일간(日干) 자체의 오행으로, 연애·직업·재물·대인관계는 십성
     * 5대 분류(TenGodGroup)의 등장 여부로 설명한다 — 지어낸 매핑이 아니라
     * 명리학에서 각 분류가 대응하는 삶의 영역을 그대로 따른 것이다.
     * 인성(학문/안정)은 이 5개 항목에 딱 맞는 영역이 없어 여기선 쓰지 않는다.
     */
    private static PersonalityProfile buildPersonalityProfile(
            String dayMaster, String yearTenGod, String monthTenGod, String timeTenGod) {
        Map<TenGodGroup, Integer> counts = new EnumMap<>(TenGodGroup.class);
        for (TenGodGroup group : TenGodGroup.values()) {
            counts.put(group, 0);
        }
        for (String tenGod : new String[] {yearTenGod, monthTenGod, timeTenGod}) {
            if (tenGod != null) {
                counts.merge(TenGodGroup.of(tenGod), 1, Integer::sum);
            }
        }

        int dayMasterGanIndex = GAN_HANGUL.indexOf(dayMaster.charAt(0));
        String dayMasterElement = String.valueOf(WUXING_HANGUL.charAt(dayMasterGanIndex / 2));

        return new PersonalityProfile(
                FiveElementTraits.describe(dayMasterElement),
                TenGodGroupTraits.describe(TenGodGroup.SIKSANG, counts.get(TenGodGroup.SIKSANG) > 0),
                TenGodGroupTraits.describe(TenGodGroup.GWANSEONG, counts.get(TenGodGroup.GWANSEONG) > 0),
                TenGodGroupTraits.describe(TenGodGroup.JAESEONG, counts.get(TenGodGroup.JAESEONG) > 0),
                TenGodGroupTraits.describe(TenGodGroup.BIGYEOP, counts.get(TenGodGroup.BIGYEOP) > 0));
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

    private static List<DaYunPeriod> toDaYunPeriods(DaYun[] daYuns) {
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

    /**
     * 오늘 기준 나이가 속한 대운 구간을 찾아 그 구간의 세운(연도별 운세) 10년을
     * 반환한다. describeCurrentDaYun(SajuReadingService)와 같은 "지금 나와 무슨
     * 상관인지" 원칙 — 전체 목록 대신 지금 걸리는 구간만 보여준다.
     */
    private static List<LiuNianPeriod> currentLiuNian(DaYun[] daYuns, String dayMaster, LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        DaYun current = null;
        for (DaYun daYun : daYuns) {
            String ganZhi = daYun.getGanZhi();
            if (ganZhi != null && !ganZhi.isEmpty() && age >= daYun.getStartAge() && age <= daYun.getEndAge()) {
                current = daYun;
                break;
            }
        }
        if (current == null) {
            return List.of();
        }

        LiuNian[] liuNians = current.getLiuNian(LIU_NIAN_COUNT);
        List<LiuNianPeriod> periods = new ArrayList<>(liuNians.length);
        for (LiuNian liuNian : liuNians) {
            String ganZhi = liuNian.getGanZhi();
            String ganHangul = toHangul(ganZhi.charAt(0), GAN_HANJA, GAN_HANGUL);
            periods.add(new LiuNianPeriod(
                    liuNian.getYear(), liuNian.getAge(), toHangulPillar(ganZhi), tenGodOfGan(dayMaster, ganHangul)));
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

    private static String toHangulTwelveStage(String hanjaStage) {
        for (int i = 0; i < TWELVE_STAGE_HANJA.length; i++) {
            if (TWELVE_STAGE_HANJA[i].equals(hanjaStage)) {
                return TWELVE_STAGE_HANGUL[i];
            }
        }
        return hanjaStage;
    }

    private static List<String> toHangulHideGan(List<String> hanjaGanList) {
        List<String> result = new ArrayList<>(hanjaGanList.size());
        for (String hanja : hanjaGanList) {
            result.add(toHangul(hanja.charAt(0), GAN_HANJA, GAN_HANGUL));
        }
        return result;
    }
}
