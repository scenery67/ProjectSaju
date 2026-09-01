package io.sj.saju.reading;

import io.sj.saju.persona.PersonaType;
import io.sj.saju.reading.dto.BreakupReadingRequest;
import io.sj.saju.reading.dto.CoupleCompatibilityRequest;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.SajuChart;
import io.sj.saju.reading.dto.SajuReadingResult;
import io.sj.saju.reading.saju.EarthlyBranchRelation;
import io.sj.saju.reading.saju.FiveElementRelation;
import io.sj.saju.reading.saju.SajuChartCalculator;
import io.sj.saju.reading.saju.TenGodTraits;
import org.springframework.stereotype.Service;

/**
 * Computes the 사주팔자 chart via {@link SajuChartCalculator} and turns it into
 * the persona-facing summary/detail text. 오행 상생상극, 십성, 일지(日支) 육합/충을
 * 반영한 규칙 기반 템플릿이다 — 형/파/해까지 반영한 정밀 궁합은 후속 작업.
 */
@Service
public class SajuReadingService {

    private final ReadingRecordRepository readingRecordRepository;

    public SajuReadingService(ReadingRecordRepository readingRecordRepository) {
        this.readingRecordRepository = readingRecordRepository;
    }

    public SajuReadingResult readBreakup(BreakupReadingRequest request) {
        PersonInput self = request.self();
        SajuChart chart = SajuChartCalculator.calculate(self);

        String summary = "%s님의 사주: %s년 %s월 %s일주, 일간(日干) %s"
                .formatted(self.name(), chart.yearPillar(), chart.monthPillar(), chart.dayPillar(), chart.dayMaster());
        String daYunLine = chart.daYunPeriods().isEmpty()
                ? ""
                : "\n첫 대운은 %d세부터 %s입니다.".formatted(
                        chart.daYunPeriods().get(0).startAge(), chart.daYunPeriods().get(0).pillar());
        String detail = """
                년주 %s / 월주 %s / 일주 %s%s
                오행 분포: %s
                오행 중 '%s' 기운이 가장 두드러집니다. 이별 이후의 감정 흐름은 이 기운의 성질을 참고해 풀이할 수 있습니다.
                월간에 나타난 '%s' 기운은 %s 모습으로 드러나곤 합니다.%s
                (형/파/해를 포함한 세부 명리학적 해석은 후속 작업으로 고도화 예정)"""
                .formatted(
                        chart.yearPillar(), chart.monthPillar(), chart.dayPillar(),
                        chart.hourPillar() != null ? " / 시주 " + chart.hourPillar() : " (출생시간 미상 — 시주 제외)",
                        chart.fiveElementCounts(), chart.dominantFiveElement(),
                        chart.monthTenGod(), TenGodTraits.describe(chart.monthTenGod()),
                        daYunLine);

        readingRecordRepository.save(new ReadingRecord(
                PersonaType.BREAKUP, self.name(), null, summary, detail));

        return new SajuReadingResult(PersonaType.BREAKUP, summary, detail, chart, null);
    }

    public SajuReadingResult readCoupleCompatibility(CoupleCompatibilityRequest request) {
        PersonInput self = request.self();
        PersonInput partner = request.partner();
        SajuChart selfChart = SajuChartCalculator.calculate(self);
        SajuChart partnerChart = SajuChartCalculator.calculate(partner);

        FiveElementRelation relation = FiveElementRelation.between(
                selfChart.dominantFiveElement(), partnerChart.dominantFiveElement());
        String relationText = switch (relation) {
            case SAME -> "두 분 모두 '" + selfChart.dominantFiveElement() + "' 기운이 강해 성향이 비슷할 수 있습니다.";
            case GENERATING -> "두 분의 기운이 서로 북돋아 주는(상생) 관계입니다.";
            case OVERCOMING -> "두 분의 기운이 서로 부딪히는(상극) 관계라 조율이 필요할 수 있습니다.";
            case UNRELATED -> "두 분의 오행 기운은 뚜렷한 상생/상극 관계가 없습니다.";
        };

        // 일지(日支)는 전통적으로 배우자궁으로 본다 — 두 사람의 일지 관계를 확인한다.
        EarthlyBranchRelation dayBranchRelation = EarthlyBranchRelation.of(
                selfChart.dayPillar().charAt(1), partnerChart.dayPillar().charAt(1));
        String dayBranchText = switch (dayBranchRelation) {
            case YUKHAP -> "두 분의 일지(배우자궁)가 육합(六合) 관계라 서로 잘 맞물리는 편입니다.";
            case CHUNG -> "두 분의 일지(배우자궁)가 충(沖) 관계라 초반엔 부딪히는 지점이 있을 수 있습니다.";
            case NONE -> "두 분의 일지(배우자궁)는 뚜렷한 합충 관계가 없습니다.";
        };

        // 상대방의 일간(日干)이 나에게 어떤 십성인지 — 궁합에서 흔히 보는 관계 지표.
        String partnerAsSeenBySelf = SajuChartCalculator.tenGodOfGan(selfChart.dayMaster(), partnerChart.dayMaster());
        String selfAsSeenByPartner = SajuChartCalculator.tenGodOfGan(partnerChart.dayMaster(), selfChart.dayMaster());

        String summary = "%s & %s님의 궁합: %s ↔ %s 오행"
                .formatted(self.name(), partner.name(), selfChart.dominantFiveElement(), partnerChart.dominantFiveElement());
        String detail = """
                %s: 년주 %s / 월주 %s / 일주 %s, 일간 %s
                %s: 년주 %s / 월주 %s / 일주 %s, 일간 %s
                %s
                %s
                %s님에게 %s님은 '%s'에 해당하고, %s님에게 %s님은 '%s'에 해당합니다.
                (형/파/해를 포함한 세부 명리학적 해석은 후속 작업으로 고도화 예정)"""
                .formatted(
                        self.name(), selfChart.yearPillar(), selfChart.monthPillar(), selfChart.dayPillar(), selfChart.dayMaster(),
                        partner.name(), partnerChart.yearPillar(), partnerChart.monthPillar(), partnerChart.dayPillar(), partnerChart.dayMaster(),
                        relationText, dayBranchText,
                        self.name(), partner.name(), partnerAsSeenBySelf,
                        partner.name(), self.name(), selfAsSeenByPartner);

        readingRecordRepository.save(new ReadingRecord(
                PersonaType.COUPLE_COMPATIBILITY, self.name(), partner.name(), summary, detail));

        return new SajuReadingResult(PersonaType.COUPLE_COMPATIBILITY, summary, detail, selfChart, partnerChart);
    }
}
