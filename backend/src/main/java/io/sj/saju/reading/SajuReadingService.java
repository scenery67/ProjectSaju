package io.sj.saju.reading;

import io.sj.saju.persona.PersonaType;
import io.sj.saju.reading.dto.BreakupReadingRequest;
import io.sj.saju.reading.dto.CoupleCompatibilityRequest;
import io.sj.saju.reading.dto.DaYunPeriod;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.ReadingHistoryEntry;
import io.sj.saju.reading.dto.SajuChart;
import io.sj.saju.reading.dto.SajuReadingResult;
import io.sj.saju.reading.saju.EarthlyBranchRelation;
import io.sj.saju.reading.saju.FiveElementRelation;
import io.sj.saju.reading.saju.FiveElementTraits;
import io.sj.saju.reading.saju.SajuChartCalculator;
import io.sj.saju.reading.saju.TenGodTraits;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Computes the 사주팔자 chart via {@link SajuChartCalculator} and turns it into
 * a plain-language reading. 계산된 값(간지/오행/십성/대운)을 그대로 나열하지 않고
 * {@link FiveElementTraits}/{@link TenGodTraits} 문구로 풀어 쓴다 — 사주 용어를
 * 모르는 사용자도 읽을 수 있어야 한다는 게 이 서비스의 핵심 요구사항이다.
 * 오행 상생상극, 십성, 일지(日支) 육합/충을 반영하며, 형/파/해까지 반영한
 * 정밀 궁합은 후속 작업.
 */
@Service
public class SajuReadingService {

    private static final Logger log = LoggerFactory.getLogger(SajuReadingService.class);
    private static final String FIVE_ELEMENT_ORDER = "목화토금수";

    private final ReadingRecordRepository readingRecordRepository;
    private final ObjectMapper objectMapper;

    public SajuReadingService(ReadingRecordRepository readingRecordRepository, ObjectMapper objectMapper) {
        this.readingRecordRepository = readingRecordRepository;
        this.objectMapper = objectMapper;
    }

    public SajuReadingResult readBreakup(BreakupReadingRequest request, UUID userAccountId) {
        PersonInput self = request.self();
        SajuChart chart = SajuChartCalculator.calculate(self);

        String summary = "%s님의 사주: %s년 %s월 %s일주, 일간(日干) %s"
                .formatted(self.name(), chart.yearPillar(), chart.monthPillar(), chart.dayPillar(), chart.dayMaster());

        String detail = """
                %s님의 사주 여덟 글자는 년주 %s · 월주 %s · 일주 %s%s예요.
                이 중 일주의 앞글자인 일간(日干) '%s'가 %s님 자신을 상징하는 기운이에요.

                사주를 이루는 다섯 기운(오행) 중에서는 %s
                그중에서도 '%s' 기운이 가장 강하게 나타나요 — %s이에요.
                이별 이후 마음이 오르내리는 결이 이 기운의 성질과 비슷하게 흘러갈 수 있어요.

                성격의 흐름을 보여주는 월주의 기운은 '%s'인데, %s이에요.

                %s

                *형(刑)·파(破)·해(害)까지 반영한 더 정밀한 해석은 준비 중이에요. 위 내용은 재미로 참고해주세요."""
                .formatted(
                        self.name(), chart.yearPillar(), chart.monthPillar(), chart.dayPillar(),
                        chart.hourPillar() != null ? " · 시주 " + chart.hourPillar() : " (시주는 출생시간 미상이라 제외했어요)",
                        chart.dayMaster(), self.name(),
                        describeFiveElementCounts(chart.fiveElementCounts()),
                        chart.dominantFiveElement(), FiveElementTraits.describe(chart.dominantFiveElement()),
                        chart.monthTenGod(), TenGodTraits.describe(chart.monthTenGod()),
                        describeCurrentDaYun(chart, self.birthDate()));

        SajuReadingResult result = new SajuReadingResult(PersonaType.BREAKUP, summary, detail, chart, null);
        readingRecordRepository.save(new ReadingRecord(
                PersonaType.BREAKUP, self.name(), null, summary, detail,
                userAccountId, resultJsonFor(userAccountId, result)));

        return result;
    }

    /**
     * 로그인 사용자 기록에만 전체 결과를 JSON으로 남긴다 — "내 사주"에서 다시
     * 열어볼 때 필요하다. 비로그인 요청(userAccountId == null)은 계속 null.
     * 직렬화가 실패해도 사주 풀이 자체는 이미 끝난 요청이라 실패시키지 않고
     * 기록만 요약 텍스트로 남긴다.
     */
    private String resultJsonFor(UUID userAccountId, SajuReadingResult result) {
        if (userAccountId == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JacksonException e) {
            log.warn("failed to serialize reading result for history, saving without it", e);
            return null;
        }
    }

    /** 로그인한 사용자의 서버 저장 사주 기록 — "내 사주" 화면에서 최신순으로 보여준다. */
    public List<ReadingHistoryEntry> history(UUID userAccountId) {
        return readingRecordRepository.findByUserAccountIdOrderByCreatedAtDesc(userAccountId).stream()
                .map(this::toHistoryEntry)
                .filter(Objects::nonNull)
                .toList();
    }

    private ReadingHistoryEntry toHistoryEntry(ReadingRecord record) {
        if (record.getResultJson() == null) {
            return null;
        }
        try {
            SajuReadingResult result = objectMapper.readValue(record.getResultJson(), SajuReadingResult.class);
            return new ReadingHistoryEntry(record.getId(), record.getCreatedAt(), result);
        } catch (JacksonException e) {
            log.warn("failed to deserialize stored reading result, skipping from history", e);
            return null;
        }
    }

    /**
     * "{목=0, 화=3, 토=4, 금=1, 수=0}" 같은 원본 데이터를 그대로 보여주지 않고,
     * 실제로 있는 기운만 자연스러운 한 문장으로 풀어 쓴다.
     */
    private static String describeFiveElementCounts(Map<String, Integer> counts) {
        Map<String, Integer> present = new LinkedHashMap<>();
        for (char c : FIVE_ELEMENT_ORDER.toCharArray()) {
            String key = String.valueOf(c);
            int count = counts.getOrDefault(key, 0);
            if (count > 0) {
                present.put(key, count);
            }
        }
        String have = present.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue() + "개")
                .collect(Collectors.joining(", "));
        String missing = FIVE_ELEMENT_ORDER.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .filter(e -> !present.containsKey(e))
                .collect(Collectors.joining(", "));
        String result = have + "가 나타나요.";
        if (!missing.isEmpty()) {
            result += " (" + missing + " 기운은 이번 사주에는 보이지 않아요)";
        }
        return result;
    }

    /**
     * 대운은 출생 시점부터 나이순으로 이어지는 목록이라, 그대로 나열하면
     * "지금 나와 무슨 상관인지" 알기 어렵다. 오늘 기준 나이에 해당하는
     * 대운을 찾아 "지금 지나고 있는 시기"로 풀어 설명한다.
     */
    private static String describeCurrentDaYun(SajuChart chart, LocalDate birthDate) {
        if (chart.daYunPeriods().isEmpty()) {
            return "";
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        DaYunPeriod current = chart.daYunPeriods().stream()
                .filter(p -> age >= p.startAge() && age <= p.endAge())
                .findFirst()
                .orElse(chart.daYunPeriods().get(0));
        return "대운은 10년마다 삶의 큰 흐름이 바뀌는 시기예요. 지금은 %d~%d세, '%s' 기운이 이어지는 시기를 지나고 있어요."
                .formatted(current.startAge(), current.endAge(), current.pillar());
    }

    public SajuReadingResult readCoupleCompatibility(CoupleCompatibilityRequest request, UUID userAccountId) {
        PersonInput self = request.self();
        PersonInput partner = request.partner();
        SajuChart selfChart = SajuChartCalculator.calculate(self);
        SajuChart partnerChart = SajuChartCalculator.calculate(partner);

        FiveElementRelation relation = FiveElementRelation.between(
                selfChart.dominantFiveElement(), partnerChart.dominantFiveElement());
        String relationText = switch (relation) {
            case SAME -> "두 분 모두 '%s' 기운(%s)이 강해서, 서로 통하는 부분이 많고 성향도 비슷하게 느껴질 수 있어요."
                    .formatted(selfChart.dominantFiveElement(), FiveElementTraits.describe(selfChart.dominantFiveElement()));
            case GENERATING -> "두 분의 기운이 서로 북돋아 주는(상생) 관계예요. 한쪽이 다른 쪽을 자연스럽게 채워주는 편안한 조합이에요.";
            case OVERCOMING -> "두 분의 기운이 서로 부딪히는(상극) 관계예요. 자극이 되기도 하지만, 초반엔 맞춰가는 노력이 필요할 수 있어요.";
            case UNRELATED -> "두 분의 오행 기운은 뚜렷한 상생/상극 관계가 없어요 — 서로에게 크게 영향을 주지 않는 무난한 조합이에요.";
        };

        // 일지(日支)는 전통적으로 배우자궁으로 본다 — 두 사람의 일지 관계를 확인한다.
        EarthlyBranchRelation dayBranchRelation = EarthlyBranchRelation.of(
                selfChart.dayPillar().charAt(1), partnerChart.dayPillar().charAt(1));
        String dayBranchText = switch (dayBranchRelation) {
            case YUKHAP -> "두 분의 일지(배우자궁)는 육합(六合) 관계예요. 서로 잘 맞물려서 자연스럽게 가까워지는 편이에요.";
            case CHUNG -> "두 분의 일지(배우자궁)는 충(沖) 관계예요. 서로 다른 방향을 보고 있어서 초반엔 부딪히는 지점이 있을 수 있어요.";
            case NONE -> "두 분의 일지(배우자궁)는 뚜렷한 합충 관계가 없어요 — 특별히 끌어당기거나 부딪히는 힘은 약한 편이에요.";
        };

        // 상대방의 일간(日干)이 나에게 어떤 십성인지 — 궁합에서 흔히 보는 관계 지표.
        String partnerAsSeenBySelf = SajuChartCalculator.tenGodOfGan(selfChart.dayMaster(), partnerChart.dayMaster());
        String selfAsSeenByPartner = SajuChartCalculator.tenGodOfGan(partnerChart.dayMaster(), selfChart.dayMaster());

        String summary = "%s & %s님의 궁합: %s ↔ %s 오행"
                .formatted(self.name(), partner.name(), selfChart.dominantFiveElement(), partnerChart.dominantFiveElement());
        String detail = """
                %s님은 '%s' 기운이, %s님은 '%s' 기운이 가장 강한 사주예요.
                %s

                %s

                두 분이 서로에게 어떤 존재로 다가오는지도 볼 수 있어요.
                %s님에게 %s님은 '%s'에 해당해요 — %s인 느낌으로 다가올 수 있어요.
                %s님에게 %s님은 '%s'에 해당해요 — %s인 느낌으로 다가올 수 있어요.

                *형(刑)·파(破)·해(害)까지 반영한 더 정밀한 궁합 해석은 준비 중이에요. 위 내용은 재미로 참고해주세요."""
                .formatted(
                        self.name(), selfChart.dominantFiveElement(), partner.name(), partnerChart.dominantFiveElement(),
                        relationText,
                        dayBranchText,
                        self.name(), partner.name(), partnerAsSeenBySelf, TenGodTraits.describe(partnerAsSeenBySelf),
                        partner.name(), self.name(), selfAsSeenByPartner, TenGodTraits.describe(selfAsSeenByPartner));

        SajuReadingResult result = new SajuReadingResult(
                PersonaType.COUPLE_COMPATIBILITY, summary, detail, selfChart, partnerChart);
        readingRecordRepository.save(new ReadingRecord(
                PersonaType.COUPLE_COMPATIBILITY, self.name(), partner.name(), summary, detail,
                userAccountId, resultJsonFor(userAccountId, result)));

        return result;
    }
}
