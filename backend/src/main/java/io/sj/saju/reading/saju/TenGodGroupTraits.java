package io.sj.saju.reading.saju;

import java.util.Map;

/**
 * TenGodGroup별 "있음/약함" 두 갈래 설명. TenGodTraits/FiveElementTraits와 같은
 * 이유로 문구를 데이터로 분리한다 (CLAUDE.md 3.6). 오락/참고용 성향 설명이다 —
 * 특정 직업·투자를 권하는 문구는 넣지 않는다.
 */
public final class TenGodGroupTraits {

    private record Pair(String present, String weak) {
    }

    private static final Map<TenGodGroup, Pair> TRAITS = Map.of(
            TenGodGroup.BIGYEOP, new Pair(
                    "주관이 뚜렷하고 자기 힘으로 관계를 이끌어가는 편이에요. 사람들 사이에서 중심이 되는 걸 편하게 느껴요.",
                    "남에게 맞춰주는 유연함이 있어서, 무리해서 주도권을 잡기보다 조화를 우선하는 편이에요."),
            TenGodGroup.SIKSANG, new Pair(
                    "표현이 풍부하고 매력을 자연스럽게 드러내는 편이라, 연애에서도 감정 표현에 적극적이에요.",
                    "감정을 겉으로 크게 드러내기보다 천천히 다가가는 스타일이라, 연애도 서두르지 않는 편이에요."),
            TenGodGroup.JAESEONG, new Pair(
                    "실속을 챙기는 감각이 있고, 기회를 실제 성과로 연결하는 능력이 있어요.",
                    "재물을 불리는 것보다 안정적으로 지키는 쪽에 더 마음이 가는 편이에요."),
            TenGodGroup.GWANSEONG, new Pair(
                    "책임감이 강하고 조직·규칙 안에서 능력을 인정받는 편이라, 소속이 있는 일에서 힘을 발휘해요.",
                    "정해진 틀보다 자유롭게 판단하고 움직이는 일에서 더 편안함을 느끼는 편이에요."),
            TenGodGroup.INSEONG, new Pair(
                    "배우고 받아들이는 힘이 좋아서, 꾸준히 공부하거나 누군가의 도움을 잘 활용하는 편이에요.",
                    "이론보다 직접 부딪히며 배우는 걸 선호하는, 실전형에 가까운 편이에요."));

    private TenGodGroupTraits() {
    }

    public static String describe(TenGodGroup group, boolean present) {
        Pair pair = TRAITS.get(group);
        return present ? pair.present() : pair.weak();
    }
}
