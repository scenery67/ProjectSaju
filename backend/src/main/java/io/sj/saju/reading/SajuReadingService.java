package io.sj.saju.reading;

import io.sj.saju.billing.CreditService;
import io.sj.saju.persona.PersonaType;
import io.sj.saju.reading.dto.BreakupReadingRequest;
import io.sj.saju.reading.dto.CoupleCompatibilityRequest;
import io.sj.saju.reading.dto.DaYunPeriod;
import io.sj.saju.reading.dto.LiuNianPeriod;
import io.sj.saju.reading.dto.PersonInput;
import io.sj.saju.reading.dto.ReadingHistoryEntry;
import io.sj.saju.reading.dto.SajuChart;
import io.sj.saju.reading.dto.SajuReadingResult;
import io.sj.saju.reading.saju.EarthlyBranchRelation;
import io.sj.saju.reading.saju.FiveElementRelation;
import io.sj.saju.reading.saju.FiveElementTraits;
import io.sj.saju.reading.saju.SajuChartCalculator;
import io.sj.saju.reading.saju.TenGodTraits;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * 정밀 궁합은 후속 작업. 결과는 여러 개의 짧은 섹션(🔹로 시작하는 소제목 +
 * 문단)을 이어 붙여 구성한다 — 사용자 피드백(참고 사이트 대비 결과가 너무
 * 짧다)에 따라 이미 계산해 둔 대운/세운 데이터를 더 적극적으로 풀어 쓴다.
 */
@Service
public class SajuReadingService {

    private static final Logger log = LoggerFactory.getLogger(SajuReadingService.class);
    private static final String FIVE_ELEMENT_ORDER = "목화토금수";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 비로그인 사용자는 서버가 추적하지 않는다(클라이언트 localStorage로만
    // 안내) — CLAUDE.md 3.2: 보관 정책을 정하기 전까지 reading_record에
    // 식별자를 추가하지 않는다는 원칙 때문에, 로그인 계정만 서버가 셀 수 있다.
    private static final int FREE_DAILY_READING_LIMIT = 2;
    private static final String CLOSING_DISCLAIMER =
            "*형(刑)·파(破)·해(害)까지 반영한 더 정밀한 해석은 준비 중이에요. 위 내용은 재미로 참고해주세요.";

    private final ReadingRecordRepository readingRecordRepository;
    private final ObjectMapper objectMapper;
    private final CreditService creditService;

    public SajuReadingService(
            ReadingRecordRepository readingRecordRepository, ObjectMapper objectMapper, CreditService creditService) {
        this.readingRecordRepository = readingRecordRepository;
        this.objectMapper = objectMapper;
        this.creditService = creditService;
    }

    /**
     * 로그인 계정이 오늘(KST 자정 기준) 이미 무료 한도를 다 썼으면 막는다.
     * useCredit이 true면 막는 대신 크레딧 1개를 소비하고 계속 진행한다.
     */
    private void enforceDailyLimit(UUID userAccountId, boolean useCredit) {
        if (userAccountId == null) {
            return;
        }
        Instant startOfTodayKst = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        long usedToday = readingRecordRepository
                .countByUserAccountIdAndCreatedAtGreaterThanEqual(userAccountId, startOfTodayKst);
        if (usedToday < FREE_DAILY_READING_LIMIT) {
            return;
        }
        if (useCredit) {
            creditService.consume(userAccountId, 1, null, "일일 무료 사주 한도 초과 후 크레딧으로 열람");
            return;
        }
        throw new DailyReadingLimitExceededException(userAccountId);
    }

    public SajuReadingResult readBreakup(BreakupReadingRequest request, UUID userAccountId, boolean useCredit) {
        enforceDailyLimit(userAccountId, useCredit);
        PersonInput self = request.self();
        SajuChart chart = SajuChartCalculator.calculate(self);

        String summary = "%s님의 사주: %s년 %s월 %s일주, 일간(日干) %s"
                .formatted(self.name(), chart.yearPillar(), chart.monthPillar(), chart.dayPillar(), chart.dayMaster());

        String detail = joinSections(
                breakupIntroSection(self, chart),
                breakupMoodSection(chart),
                breakupDaYunSection(chart, self.birthDate()),
                breakupLiuNianSection(chart),
                CLOSING_DISCLAIMER);

        // result가 자기 자신의 reading_record id를 담아야 해서(LLM 상담 세션이
        // 이 값으로 결과를 참조) 먼저 저장해 id를 받은 뒤 그 id가 포함된
        // JSON을 다시 채워 넣는다.
        ReadingRecord record = readingRecordRepository.save(new ReadingRecord(
                PersonaType.BREAKUP, self.name(), null, summary, detail, userAccountId, null));
        SajuReadingResult result = new SajuReadingResult(
                record.getId(), PersonaType.BREAKUP, summary, detail, chart, null);
        if (userAccountId != null) {
            record.setResultJson(resultJsonFor(userAccountId, result));
            readingRecordRepository.save(record);
        }

        return result;
    }

    private static String breakupIntroSection(PersonInput self, SajuChart chart) {
        return """
                🔹 사주 기본 구성
                %s님의 사주 여덟 글자는 년주 %s · 월주 %s · 일주 %s%s예요.
                이 중 일주의 앞글자인 일간(日干) '%s'가 %s님 자신을 상징하는 기운이에요.

                사주를 이루는 다섯 기운(오행) 중에서는 %s
                그중에서도 '%s' 기운이 가장 강하게 나타나요 — %s이에요."""
                .formatted(
                        self.name(), chart.yearPillar(), chart.monthPillar(), chart.dayPillar(),
                        chart.hourPillar() != null ? " · 시주 " + chart.hourPillar() : " (시주는 출생시간 미상이라 제외했어요)",
                        chart.dayMaster(), self.name(),
                        describeFiveElementCounts(chart.fiveElementCounts()),
                        chart.dominantFiveElement(), FiveElementTraits.describe(chart.dominantFiveElement()));
    }

    private static String breakupMoodSection(SajuChart chart) {
        return """
                🔹 이별 이후 마음이 흘러가는 결
                이별 이후 마음이 오르내리는 결이 앞서 본 '%s' 기운의 성질과 비슷하게 흘러갈 수 있어요.
                성격의 흐름을 보여주는 월주의 기운은 '%s'인데, %s이에요."""
                .formatted(chart.dominantFiveElement(), chart.monthTenGod(), TenGodTraits.describe(chart.monthTenGod()));
    }

    private static String breakupDaYunSection(SajuChart chart, LocalDate birthDate) {
        int index = currentDaYunIndex(chart, birthDate);
        if (index < 0) {
            return "";
        }
        List<DaYunPeriod> periods = chart.daYunPeriods();
        DaYunPeriod current = periods.get(index);
        DaYunPeriod next = index + 1 < periods.size() ? periods.get(index + 1) : null;

        String body = "대운은 10년마다 삶의 큰 흐름이 바뀌는 시기예요. 지금은 %d~%d세, '%s' 기운이 이어지는 시기를 지나고 있어요."
                .formatted(current.startAge(), current.endAge(), current.pillar());
        if (next != null) {
            body += " 이 흐름이 지나면 %d세부터는 '%s' 기운의 대운으로 넘어가요.".formatted(next.startAge(), next.pillar());
        }
        return "🔹 삶의 큰 흐름 (대운)\n" + body;
    }

    /** 대운 10년 안에서 연도별로 들어오는 세운 중, 올해 이후 것만 최대 3개 골라 풀어 쓴다. */
    private static String breakupLiuNianSection(SajuChart chart) {
        int thisYear = LocalDate.now().getYear();
        List<LiuNianPeriod> upcoming = chart.currentLiuNian().stream()
                .filter(p -> p.year() >= thisYear)
                .limit(3)
                .toList();
        if (upcoming.isEmpty()) {
            return "";
        }
        String lines = upcoming.stream()
                .map(p -> "%d년(%s, %d세)은 '%s' 기운이 들어오는 해예요 — %s"
                        .formatted(p.year(), p.pillar(), p.age(), p.tenGod(), TenGodTraits.describe(p.tenGod())))
                .collect(Collectors.joining("\n"));
        return "🔹 요즘과 앞으로의 흐름 (세운)\n" + lines;
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

    /** 빈 섹션(예: 대운/세운 데이터가 없는 경우)은 걸러내고 빈 줄 하나로 이어 붙인다. */
    private static String joinSections(String... sections) {
        return List.of(sections).stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n"));
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
     * "지금 나와 무슨 상관인지" 알기 어렵다. 오늘 기준 나이가 속한 대운의
     * 인덱스를 찾는다 — 못 찾으면(데이터 없음) -1, 목록은 있는데 매칭 구간이
     * 없으면 0(첫 구간)을 돌려준다.
     */
    private static int currentDaYunIndex(SajuChart chart, LocalDate birthDate) {
        List<DaYunPeriod> periods = chart.daYunPeriods();
        if (periods.isEmpty()) {
            return -1;
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        for (int i = 0; i < periods.size(); i++) {
            DaYunPeriod p = periods.get(i);
            if (age >= p.startAge() && age <= p.endAge()) {
                return i;
            }
        }
        return 0;
    }

    public SajuReadingResult readCoupleCompatibility(
            CoupleCompatibilityRequest request, UUID userAccountId, boolean useCredit) {
        enforceDailyLimit(userAccountId, useCredit);
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

        String detail = joinSections(
                coupleElementSection(self, partner, selfChart, partnerChart, relationText),
                "🔹 배우자궁 궁합\n" + dayBranchText,
                coupleTenGodSection(self.name(), partner.name(), partnerAsSeenBySelf, selfAsSeenByPartner),
                coupleLiuNianSection(self.name(), partner.name(), selfChart, partnerChart),
                coupleDaYunSection(self.name(), partner.name(), selfChart, partnerChart, self.birthDate(), partner.birthDate()),
                CLOSING_DISCLAIMER.replace("해석은", "궁합 해석은"));

        ReadingRecord record = readingRecordRepository.save(new ReadingRecord(
                PersonaType.COUPLE_COMPATIBILITY, self.name(), partner.name(), summary, detail,
                userAccountId, null));
        SajuReadingResult result = new SajuReadingResult(
                record.getId(), PersonaType.COUPLE_COMPATIBILITY, summary, detail, selfChart, partnerChart);
        if (userAccountId != null) {
            record.setResultJson(resultJsonFor(userAccountId, result));
            readingRecordRepository.save(record);
        }

        return result;
    }

    private static String coupleElementSection(
            PersonInput self, PersonInput partner, SajuChart selfChart, SajuChart partnerChart, String relationText) {
        return """
                🔹 두 분의 오행 기운
                %s님은 '%s' 기운이, %s님은 '%s' 기운이 가장 강한 사주예요.
                %s"""
                .formatted(
                        self.name(), selfChart.dominantFiveElement(), partner.name(), partnerChart.dominantFiveElement(),
                        relationText);
    }

    private static String coupleTenGodSection(
            String selfName, String partnerName, String partnerAsSeenBySelf, String selfAsSeenByPartner) {
        return """
                🔹 서로에게 어떤 존재로 다가오나요
                %s님에게 %s님은 '%s'에 해당해요 — %s인 느낌으로 다가올 수 있어요.
                %s님에게 %s님은 '%s'에 해당해요 — %s인 느낌으로 다가올 수 있어요."""
                .formatted(
                        selfName, partnerName, partnerAsSeenBySelf, TenGodTraits.describe(partnerAsSeenBySelf),
                        partnerName, selfName, selfAsSeenByPartner, TenGodTraits.describe(selfAsSeenByPartner));
    }

    /** 두 사람 모두에게 올해(세운)에 들어온 기운을 나란히 짚어준다 — 없으면(연산 범위 밖) 섹션째 생략. */
    private static String coupleLiuNianSection(
            String selfName, String partnerName, SajuChart selfChart, SajuChart partnerChart) {
        int thisYear = LocalDate.now().getYear();
        Optional<LiuNianPeriod> selfNow = findYear(selfChart, thisYear);
        Optional<LiuNianPeriod> partnerNow = findYear(partnerChart, thisYear);
        if (selfNow.isEmpty() || partnerNow.isEmpty()) {
            return "";
        }
        return """
                🔹 요즘 두 분에게 들어온 기운 (세운)
                %d년, %s님에게는 '%s' 기운이(%s), %s님에게는 '%s' 기운이(%s) 들어와 있어요.
                같은 해를 지나고 있어도 서로 받는 기운은 다를 수 있어요 — 요즘 서로 속도 차이가 느껴진다면 이 때문일 수 있어요."""
                .formatted(
                        thisYear, selfName, selfNow.get().tenGod(), TenGodTraits.describe(selfNow.get().tenGod()),
                        partnerName, partnerNow.get().tenGod(), TenGodTraits.describe(partnerNow.get().tenGod()));
    }

    private static Optional<LiuNianPeriod> findYear(SajuChart chart, int year) {
        return chart.currentLiuNian().stream().filter(p -> p.year() == year).findFirst();
    }

    /** 두 사람이 지금 각자 어떤 대운(10년 흐름) 안에 있는지 나란히 짚어준다. */
    private static String coupleDaYunSection(
            String selfName, String partnerName, SajuChart selfChart, SajuChart partnerChart,
            LocalDate selfBirthDate, LocalDate partnerBirthDate) {
        int selfIndex = currentDaYunIndex(selfChart, selfBirthDate);
        int partnerIndex = currentDaYunIndex(partnerChart, partnerBirthDate);
        if (selfIndex < 0 || partnerIndex < 0) {
            return "";
        }
        DaYunPeriod selfCurrent = selfChart.daYunPeriods().get(selfIndex);
        DaYunPeriod partnerCurrent = partnerChart.daYunPeriods().get(partnerIndex);
        return """
                🔹 지금 두 분이 지나는 큰 흐름 (대운)
                %s님은 %d~%d세 구간, '%s' 기운의 대운을 지나고 있고
                %s님은 %d~%d세 구간, '%s' 기운의 대운을 지나고 있어요.
                서로 다른 인생 흐름 위에 있다는 걸 이해하면, 요즘 생활 리듬이나 관심사 차이도 자연스럽게 받아들여질 수 있어요."""
                .formatted(
                        selfName, selfCurrent.startAge(), selfCurrent.endAge(), selfCurrent.pillar(),
                        partnerName, partnerCurrent.startAge(), partnerCurrent.endAge(), partnerCurrent.pillar());
    }
}
