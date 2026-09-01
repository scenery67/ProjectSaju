package io.sj.saju.reading.saju;

import java.util.Map;

/**
 * 오행(五行) 5종에 대한 짧은 성향 설명. TenGodTraits와 같은 이유로 문구를
 * 데이터로만 관리한다 (CLAUDE.md 3.6) — 오락/참고용 성향 설명일 뿐이다.
 */
public final class FiveElementTraits {

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "목", "새로운 걸 시작하고 성장시키려는, 진취적이고 곧은 기운",
            "화", "감정 표현이 풍부하고 열정적으로 움직이는 기운",
            "토", "중심을 잡고 관계를 안정시키려는, 묵직하고 신뢰감 있는 기운",
            "금", "맺고 끊음이 분명하고 원칙을 지키려는 기운",
            "수", "상황에 유연하게 흐르며 생각이 깊어지는 기운");

    private FiveElementTraits() {
    }

    public static String describe(String hangulElement) {
        return DESCRIPTIONS.getOrDefault(hangulElement, "");
    }
}
