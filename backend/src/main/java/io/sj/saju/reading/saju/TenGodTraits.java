package io.sj.saju.reading.saju;

import java.util.Map;

/**
 * 십성(十神) 10종에 대한 짧은 성향 설명. 해석 문구는 여기 데이터로만 관리하고
 * 서비스 로직에는 하드코딩하지 않는다 (CLAUDE.md 3.6).
 * 오락/참고용 성향 설명일 뿐, 재무·의료·법률적 조언으로 읽히는 문구는 넣지 않는다.
 */
public final class TenGodTraits {

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry("비견", "자기 주관이 뚜렷하고 독립적으로 움직이려는 성향"),
            Map.entry("겁재", "추진력과 경쟁심이 강하고 승부에 적극적인 성향"),
            Map.entry("식신", "여유롭고 표현이 부드러우며 나누는 걸 좋아하는 성향"),
            Map.entry("상관", "재치 있고 자유분방하며 틀에 얽매이길 싫어하는 성향"),
            Map.entry("편재", "사교적이고 융통성 있게 상황에 적응하는 성향"),
            Map.entry("정재", "성실하고 계획적으로 꾸준히 쌓아가는 성향"),
            Map.entry("편관", "추진력과 승부욕이 강하지만 스트레스 관리가 필요한 성향"),
            Map.entry("정관", "책임감이 강하고 원칙과 질서를 중시하는 성향"),
            Map.entry("편인", "독창적이고 직관적인 사고를 하는 성향"),
            Map.entry("정인", "학습·수용력이 좋고 안정을 추구하는 성향"));

    private TenGodTraits() {
    }

    public static String describe(String hangulTenGod) {
        return DESCRIPTIONS.getOrDefault(hangulTenGod, "");
    }
}
